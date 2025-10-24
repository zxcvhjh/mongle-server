package com.algangi.mongle.post.application.service;

import java.util.List;
import java.util.Optional;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.algangi.mongle.comment.domain.model.Comment;
import com.algangi.mongle.comment.domain.repository.CommentRepository;
import com.algangi.mongle.dynamicCloud.domain.model.DynamicCloud;
import com.algangi.mongle.dynamicCloud.domain.repository.DynamicCloudRepository;
import com.algangi.mongle.dynamicCloud.domain.service.DynamicCloudFormationService;
import com.algangi.mongle.global.domain.service.CellService;
import com.algangi.mongle.global.exception.ApplicationException;
import com.algangi.mongle.member.application.service.MemberFinder;
import com.algangi.mongle.member.domain.model.Member;
import com.algangi.mongle.member.domain.model.MemberStatus;
import com.algangi.mongle.member.domain.repository.MemberRepository;
import com.algangi.mongle.member.exception.MemberErrorCode;
import com.algangi.mongle.post.application.dto.PostCreationCommand;
import com.algangi.mongle.post.domain.model.Location;
import com.algangi.mongle.post.domain.model.Post;
import com.algangi.mongle.post.domain.model.PostStatus;
import com.algangi.mongle.post.domain.repository.PostRepository;
import com.algangi.mongle.post.domain.service.LocationRandomizer;
import com.algangi.mongle.post.event.PostCreatedEvent;
import com.algangi.mongle.post.presentation.dto.PostCreateRequest;
import com.algangi.mongle.post.presentation.dto.PostCreateResponse;
import com.algangi.mongle.staticCloud.domain.model.StaticCloud;
import com.algangi.mongle.staticCloud.repository.StaticCloudRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class PostCreationService {

    private static final int DYNAMIC_CLOUD_CREATION_THRESHOLD = 2;
    private static final int MAX_POST_COUNT_PER_USER = 5;
    private final StaticCloudRepository staticCloudRepository;
    private final DynamicCloudRepository dynamicCloudRepository;
    private final PostRepository postRepository;
    private final DynamicCloudFormationService dynamicCloudFormationService;
    private final ApplicationEventPublisher eventPublisher;
    private final MemberFinder memberFinder;
    private final LocationRandomizer locationRandomizer;
    private final CellService cellService;
    private final PostRateLimiter postRateLimiter;
    private final CommentRepository commentRepository;
    private final MemberRepository memberRepository;

    @Transactional
    public PostCreateResponse createPost(PostCreateRequest request, String authorId) {
        // 회원에 락을 걸어 동시성 처리 보완 (동일한 사용자 요청의 경우 락 걸림)
        Member author = memberFinder.getMemberWithLockOrThrow(authorId);

        requireActive(author);

        // 관리자가 아닌 경우에만 3분 글쓰기 제한 적용
        if (!author.isAdmin()) {
            postRateLimiter.checkRateLimit(authorId);
        }

        // 회원당 게시물 최대 5개 유지
        long existingPostCount = postRepository.countByAuthorIdAndStatus(authorId,
            PostStatus.ACTIVE);
        if (existingPostCount >= MAX_POST_COUNT_PER_USER) {
            Optional<Post> oldestPost = postRepository.findFirstByAuthorIdAndStatusOrderByCreatedDateAsc(
                authorId, PostStatus.ACTIVE);
            oldestPost.ifPresent(Post::softDeleteByAdmin);
        }

        boolean isAnonymous = request.isAnonymous() != null && request.isAnonymous();

        Location originalLocation = Location.create(request.latitude(), request.longitude());
        String originalS2TokenId = cellService.generateS2TokenIdFrom(originalLocation.getLatitude(),
            originalLocation.getLongitude());
        Optional<StaticCloud> staticCloud = staticCloudRepository.findByS2TokenId(
            originalS2TokenId);

        Location finalLocation = originalLocation;
        if (request.isRandomLocationEnabled() && staticCloud.isEmpty()) {
            finalLocation = locationRandomizer.randomize(originalLocation);
        }

        String finalS2TokenId = cellService.generateS2TokenIdFrom(finalLocation.getLatitude(),
            finalLocation.getLongitude());

        PostCreationCommand command = PostCreationCommand.of(
            finalLocation,
            finalS2TokenId,
            request.content(),
            authorId,
            isAnonymous);

        Post createdPost;
        Optional<DynamicCloud> existingDynamicCloud = dynamicCloudRepository.findActiveByS2TokenId(
            finalS2TokenId);
        // 1. 정적 구름 존재 여부 확인
        if (staticCloud.isPresent()) {
            createdPost = createPostInStaticCloud(command, staticCloud.get());
        }
        // 2. 동적 구름 존재 여부 확인
        else if (existingDynamicCloud.isPresent()) {
            createdPost = createPostInDynamicCloud(command, existingDynamicCloud.get());
        }
        // 3. 동적 구름이 없는 경우
        else {
            createdPost = handleNewPost(command, finalS2TokenId);
        }
        Post savedPost = postRepository.save(createdPost);
        if (!author.isAdmin()) {
            postRateLimiter.blockUser(authorId);
        }

        // TODO: 추후 변경 예정
        Member admin = memberFinder.getMemberOrThrow("admin_01");
        long currentPostCount =
            existingPostCount + 1 <= MAX_POST_COUNT_PER_USER ? existingPostCount + 1
                : MAX_POST_COUNT_PER_USER;
        String content = String.format("게시글 수 (%d/%d)", currentPostCount, MAX_POST_COUNT_PER_USER);
        Comment noifyComment = Comment.createParentComment(content, savedPost, admin,
            isAnonymous);
        commentRepository.save(noifyComment);

        eventPublisher.publishEvent(
            new PostCreatedEvent(savedPost.getId(), request.fileKeyList()));
        return PostCreateResponse.from(savedPost);
    }

    private Post handleNewPost(PostCreationCommand command, String s2TokenId) {
        List<Post> existingPostsInCell = postRepository.findByS2TokenIdWithLock(s2TokenId);
        // Concurrency Gap 방지 로직
        Optional<DynamicCloud> cloudAfterLock = dynamicCloudRepository.findActiveByS2TokenId(
            s2TokenId);
        if (cloudAfterLock.isPresent()) {
            return createPostInDynamicCloud(command, cloudAfterLock.get());
        }

        // 게시물 개수에 따른 처리
        int totalPostCount = existingPostsInCell.size() + 1;
        if (totalPostCount <= DYNAMIC_CLOUD_CREATION_THRESHOLD) {
            return createStandalonePost(command);
        } else {
            // DynamicCloudService에 동적 구름 생성 및 병합 책임을 위임
            DynamicCloud targetCloud = dynamicCloudFormationService.createDynamicCloudAndMergeIfNeeded(
                s2TokenId, existingPostsInCell);
            return createPostInDynamicCloud(command, targetCloud);
        }
    }


    //게시물 생성 헬퍼 메서드
    private Post createPostInStaticCloud(PostCreationCommand command, StaticCloud staticCloud) {
        return Post.createInStaticCloud(
            command.location(),
            command.s2TokenId(),
            command.content(),
            command.authorId(),
            staticCloud.getId(),
            command.isAnonymous()
        );
    }

    private Post createPostInDynamicCloud(PostCreationCommand command, DynamicCloud dynamicCloud) {
        return Post.createInDynamicCloud(
            command.location(),
            command.s2TokenId(),
            command.content(),
            command.authorId(),
            dynamicCloud.getId(),
            command.isAnonymous()
        );
    }

    private Post createStandalonePost(PostCreationCommand command) {
        return Post.createStandalone(
            command.location(),
            command.s2TokenId(),
            command.content(),
            command.authorId(),
            command.isAnonymous()
        );
    }

    private void requireActive(Member member) {
        if (member.getStatus() == MemberStatus.BANNED) {
            throw new ApplicationException(MemberErrorCode.MEMBER_IS_BANNED);
        }
        if (member.getStatus() == MemberStatus.DEACTIVATED) {
            throw new ApplicationException(MemberErrorCode.MEMBER_IS_DEACTIVATED);
        }
    }
}