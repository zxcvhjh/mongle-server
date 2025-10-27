package com.algangi.mongle.post.application.service;

import com.algangi.mongle.comment.application.service.NotifyBotCommentService;
import com.algangi.mongle.dynamicCloud.domain.model.DynamicCloud;
import com.algangi.mongle.dynamicCloud.domain.repository.DynamicCloudRepository;
import com.algangi.mongle.dynamicCloud.domain.service.DynamicCloudFormationService;
import com.algangi.mongle.global.domain.service.CellService;
import com.algangi.mongle.global.exception.ApplicationException;
import com.algangi.mongle.member.application.service.MemberFinder;
import com.algangi.mongle.member.domain.model.Member;
import com.algangi.mongle.member.domain.model.MemberStatus;
import com.algangi.mongle.member.exception.MemberErrorCode;
import com.algangi.mongle.post.application.dto.PostCreationCommand;
import com.algangi.mongle.post.domain.model.Location;
import com.algangi.mongle.post.domain.model.Post;
import com.algangi.mongle.post.domain.model.PostStatus;
import com.algangi.mongle.post.domain.repository.PostRepository;
import com.algangi.mongle.post.domain.service.LocationRandomizer;
import com.algangi.mongle.post.event.PostCreatedEvent;
import com.algangi.mongle.post.exception.PostErrorCode;
import com.algangi.mongle.post.presentation.dto.PostCreateRequest;
import com.algangi.mongle.post.presentation.dto.PostCreateResponse;
import com.algangi.mongle.staticCloud.domain.model.StaticCloud;
import com.algangi.mongle.staticCloud.repository.StaticCloudRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

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
    private final NotifyBotCommentService notifyBotCommentService;

    @Transactional
    public PostCreateResponse createPost(PostCreateRequest request, String authorId) {
        Member author = memberFinder.getMemberWithLockOrThrow(authorId);

        // !! 관리자 차단 로직 제거 !!
        // if (author.isAdmin()) {
        //      log.warn("Admin user attempted to create post via user endpoint. userId={}", authorId);
        //      throw new ApplicationException(PostErrorCode.POST_ACCESS_DENIED, "Admins must use the admin endpoint.");
        // }

        // 부스 계정 처리 (기존 로직 유지)
        if (author.isBooth()) {
            if (postRepository.countByAuthorIdAndStatus(authorId, PostStatus.ACTIVE) >= 1) {
                throw new ApplicationException(PostErrorCode.BOOTH_POST_MAXIMUM_EXCEED);
            }
            Post boothPost = createNonExpiredStandaloneForBooth(request, authorId);
            Post savedBoothPost = postRepository.save(boothPost);
            eventPublisher.publishEvent(
                new PostCreatedEvent(savedBoothPost.getId(), request.fileKeyList()));
            return PostCreateResponse.from(savedBoothPost);
        }

        // 일반 사용자 및 관리자 공통 처리 로직 시작
        requireActive(author);

        // 관리자가 아닌 경우에만 속도 제한 적용 (관리자는 제한 없음)
        if (!author.isAdmin()) {
            postRateLimiter.checkRateLimit(authorId);
        }

        // 관리자가 아닌 경우에만 게시글 수 제한 적용 (관리자는 제한 없음)
        long existingPostCount = 0;
        if (!author.isAdmin()) {
            existingPostCount = postRepository.countByAuthorIdAndStatus(authorId,
                PostStatus.ACTIVE);
            if (existingPostCount >= MAX_POST_COUNT_PER_USER) {
                Optional<Post> oldestPost = postRepository.findFirstByAuthorIdAndStatusOrderByCreatedDateAsc(
                    authorId, PostStatus.ACTIVE);
                // 일반 사용자 글만 만료 처리 (관리자 글은 건드리지 않음)
                oldestPost.ifPresent(post -> {
                    if (!post.getAuthorId().startsWith("admin")) { // 방어 로직 추가
                        post.markAsExpired(); // 상태 변경 (혹은 softDeleteByAdmin 등 정책에 맞게)
                    }
                });
            }
        }

        boolean isAnonymous = request.isAnonymous() != null && request.isAnonymous();

        Location originalLocation = Location.create(request.latitude(), request.longitude());
        String originalS2TokenId = cellService.generateS2TokenIdFrom(originalLocation.getLatitude(),
            originalLocation.getLongitude());
        Optional<StaticCloud> staticCloud = staticCloudRepository.findByS2TokenId(
            originalS2TokenId);

        Location finalLocation = originalLocation;
        // 관리자가 아니고, 랜덤 위치 옵션 활성화 시 + 정적 구름 아닐 때만 랜덤화
        if (!author.isAdmin() && request.isRandomLocationEnabled() && staticCloud.isEmpty()) {
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

        if (staticCloud.isPresent()) {
            createdPost = createPostInStaticCloud(command, staticCloud.get());
        } else if (existingDynamicCloud.isPresent()) {
            createdPost = createPostInDynamicCloud(command, existingDynamicCloud.get());
        } else {
            createdPost = handleNewPost(command, finalS2TokenId);
        }

        Post savedPost = postRepository.save(createdPost);

        // 관리자가 아닌 경우에만 속도 제한 블록 적용
        if (!author.isAdmin()) {
            postRateLimiter.blockUser(authorId);
        }

        // 알림 봇 댓글 추가 (관리자가 아닌 일반 사용자에게만)
        if (!author.isAdmin()) {
            long currentPostCount = existingPostCount < MAX_POST_COUNT_PER_USER ? existingPostCount + 1
                : MAX_POST_COUNT_PER_USER;

            String notifyContent = String.format(
                "⚙\uFE0F 게시글 (%d/%d) · 5개 초과시 가장 오래된 게시글 삭제" +
                    "\n⚙\uFE0F 24시간 후 게시글 자동 삭제",
                currentPostCount,
                MAX_POST_COUNT_PER_USER);

            notifyBotCommentService.notifyByComment(notifyContent, savedPost);
        }

        eventPublisher.publishEvent(new PostCreatedEvent(savedPost.getId(), request.fileKeyList()));
        return PostCreateResponse.from(savedPost);
    }

    private Post handleNewPost(PostCreationCommand command, String s2TokenId) {
        List<Post> existingPostsInCell = postRepository.findByS2TokenIdWithLock(s2TokenId);

        Optional<DynamicCloud> cloudAfterLock = dynamicCloudRepository.findActiveByS2TokenId(
            s2TokenId);
        if (cloudAfterLock.isPresent()) {
            return createPostInDynamicCloud(command, cloudAfterLock.get());
        }

        // 관리자 글은 동적 구름 생성 조건 카운트에서 제외할지 여부 결정 필요
        // 현재: 관리자 글도 카운트에 포함하여 동적 구름 생성 판단
        int totalPostCount = existingPostsInCell.size() + 1;
        if (totalPostCount <= DYNAMIC_CLOUD_CREATION_THRESHOLD) {
            return createStandalonePost(command);
        } else {
            DynamicCloud targetCloud = dynamicCloudFormationService.createDynamicCloudAndMergeIfNeeded(
                s2TokenId, existingPostsInCell);
            return createPostInDynamicCloud(command, targetCloud);
        }
    }


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

    private Post createNonExpiredStandaloneForBooth(PostCreateRequest request, String authorId) {
        String s2TokenId = cellService.generateS2TokenIdFrom(request.latitude(),
            request.longitude());
        // Booth post creation doesn't need infoText or customNickname
        return Post.createNonExpiredStandalone(
            Location.create(request.latitude(), request.longitude()),
            s2TokenId,
            request.content(),
            authorId,
            false,
            null,
            null
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

