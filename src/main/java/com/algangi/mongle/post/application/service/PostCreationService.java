package com.algangi.mongle.post.application.service;

import java.util.List;
import java.util.Optional;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.algangi.mongle.comment.application.service.NotifyBotCommentService;
import com.algangi.mongle.dynamicCloud.domain.model.DynamicCloud;
import com.algangi.mongle.dynamicCloud.domain.repository.DynamicCloudRepository;
import com.algangi.mongle.dynamicCloud.domain.service.DynamicCloudFormationService;
import com.algangi.mongle.global.domain.service.CellService;
import com.algangi.mongle.global.exception.ApplicationException;
import com.algangi.mongle.member.application.service.MemberFinder;
import com.algangi.mongle.member.domain.model.Member;
import com.algangi.mongle.member.domain.model.MemberRole;
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
import com.algangi.mongle.post.presentation.dto.AdminPostCreateRequest;
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
    private final NotifyBotCommentService notifyBotCommentService;

    @Transactional
    public PostCreateResponse createPost(PostCreateRequest request, String authorId) {
        Member author = memberFinder.getMemberWithLockOrThrow(authorId);

        if (author.isAdmin()) {
            log.warn("관리자 계정({})이 일반 사용자용 게시글 생성 API를 호출했습니다. 관리자용 API 사용을 권장합니다.", authorId);
            AdminPostCreateRequest adminRequest = new AdminPostCreateRequest(
                request.latitude(), request.longitude(), request.content(),
                null,
                request.fileKeyList(), request.isRandomLocationEnabled(), request.isAnonymous()
            );
            return createAdminPost(adminRequest, authorId);
        }

        if (author.isBooth()) {
            if (postRepository.countByAuthorIdAndStatus(authorId, PostStatus.ACTIVE) >= 1) {
                throw new ApplicationException(PostErrorCode.BOOTH_POST_MAXIMUM_EXCEED);
            }
            Post boothPost = createNonExpiredStandalone(
                Location.create(request.latitude(), request.longitude()),
                cellService.generateS2TokenIdFrom(request.latitude(), request.longitude()),
                request.content(),
                authorId,
                Boolean.TRUE.equals(request.isAnonymous()),
                null
            );

            postRepository.save(boothPost);
            eventPublisher.publishEvent(
                new PostCreatedEvent(boothPost.getId(), request.fileKeyList()));
            return PostCreateResponse.from(boothPost);
        }

        requireActive(author);

        postRateLimiter.checkRateLimit(authorId);

        long existingPostCount = postRepository.countByAuthorIdAndStatus(authorId,
            PostStatus.ACTIVE);
        if (existingPostCount >= MAX_POST_COUNT_PER_USER) {
            Optional<Post> oldestPost = postRepository.findFirstByAuthorIdAndStatusOrderByCreatedDateAsc(
                authorId, PostStatus.ACTIVE);
            oldestPost.ifPresent(Post::softDeleteByUser);
        }

        boolean isAnonymous = Boolean.TRUE.equals(request.isAnonymous());

        Location originalLocation = Location.create(request.latitude(), request.longitude());
        String originalS2TokenId = cellService.generateS2TokenIdFrom(originalLocation.getLatitude(),
            originalLocation.getLongitude());
        Optional<StaticCloud> staticCloud = staticCloudRepository.findByS2TokenId(
            originalS2TokenId);

        Location finalLocation = originalLocation;
        if (Boolean.TRUE.equals(request.isRandomLocationEnabled()) && staticCloud.isEmpty()) {
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

        postRateLimiter.blockUser(authorId);

        long currentPostCount =
            existingPostCount + 1 <= MAX_POST_COUNT_PER_USER ? existingPostCount + 1
                : MAX_POST_COUNT_PER_USER;
        String content = String.format(
            "⚙\uFE0F 게시글 (%d/%d) · 5개 초과시 가장 오래된 게시글 삭제"
                + "\n⚙\uFE0F 24시간 후 게시글은 자동 삭제됩니다.",
            currentPostCount,
            MAX_POST_COUNT_PER_USER);
        notifyBotCommentService.notifyByComment(content, savedPost);

        eventPublisher.publishEvent(new PostCreatedEvent(savedPost.getId(), request.fileKeyList()));
        return PostCreateResponse.from(savedPost);
    }


    @Transactional
    public PostCreateResponse createAdminPost(AdminPostCreateRequest request, String authorId) {
        Member author = memberFinder.getMemberOrThrow(authorId);
        if (author.getMemberRole() != MemberRole.ADMIN) {
            throw new ApplicationException(MemberErrorCode.MEMBER_IS_BANNED);
        }

        Location location = Location.create(request.latitude(), request.longitude());
        String s2TokenId = cellService.generateS2TokenIdFrom(location.getLatitude(),
            location.getLongitude());

        Post adminPost = Post.createNonExpiredStandalone(
            location,
            s2TokenId,
            request.content(),
            authorId,
            request.isAnonymous(),
            request.infoText()
        );

        Post savedPost = postRepository.save(adminPost);

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

    private Post createNonExpiredStandalone(
        Location location,
        String s2TokenId,
        String content,
        String authorId,
        boolean isAnonymous,
        String infoText
    ) {
        return Post.createNonExpiredStandalone(
            location,
            s2TokenId,
            content,
            authorId,
            isAnonymous,
            infoText
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

