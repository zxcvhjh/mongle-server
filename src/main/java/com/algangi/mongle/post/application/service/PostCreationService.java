package com.algangi.mongle.post.application.service;

import com.algangi.mongle.dynamicCloud.domain.repository.DynamicCloudRepository;
import com.algangi.mongle.member.application.service.MemberFinder;
import com.algangi.mongle.member.domain.model.Member;
import com.algangi.mongle.post.application.dto.PostCreationCommand;
import com.algangi.mongle.post.domain.model.Location;
import com.algangi.mongle.post.domain.model.Post;
import com.algangi.mongle.post.domain.repository.PostRepository;
import com.algangi.mongle.post.event.PostCreatedEvent;
import com.algangi.mongle.post.presentation.dto.PostCreateRequest;
import com.algangi.mongle.post.presentation.dto.PostCreateResponse;
import com.algangi.mongle.staticCloud.domain.model.StaticCloud;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * 게시글 생성 서비스
 * 책임: 게시글 생성 오케스트레이션
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PostCreationService {

    private final PostRepository postRepository;
    private final DynamicCloudRepository dynamicCloudRepository;
    private final MemberFinder memberFinder;
    private final ApplicationEventPublisher eventPublisher;

    // 분리된 책임 서비스들
    private final PostCreationPolicyService policyService;
    private final PostLocationService locationService;
    private final PostNotificationService notificationService;

    @Transactional
    public PostCreateResponse createPost(PostCreateRequest request, String authorId) {
        // 1. 작성자 조회 및 검증
        Member author = memberFinder.getMemberWithLockOrThrow(authorId);
        policyService.validateMemberStatus(author);

        // 2. 부스 계정 전용 처리
        if (author.isBooth()) {
            return createBoothPost(request, authorId);
        }

        // 3. 정책 검증 (Rate limiting, 게시글 수 제한)
        policyService.checkRateLimit(author);
        long existingPostCount = policyService.checkAndHandlePostCountLimit(author);

        // 4. 위치 처리
        Location originalLocation = Location.create(request.latitude(), request.longitude());
        boolean isRandomLocationEnabled = request.isRandomLocationEnabled();
        PostLocationService.LocationProcessingResult locationResult =
            locationService.processLocation(originalLocation, author, isRandomLocationEnabled);

        // 5. 게시글 생성
        boolean isAnonymous = request.isAnonymous() != null && request.isAnonymous();
        PostCreationCommand command = PostCreationCommand.of(
            locationResult.finalLocation(),
            locationResult.s2TokenId(),
            request.content(),
            authorId,
            isAnonymous
        );

        Post createdPost = createPostBasedOnCloudType(command, locationResult.staticCloud());
        Post savedPost = postRepository.save(createdPost);

        // 6. 후처리 (Rate limit 블록, 알림, 이벤트)
        policyService.applyRateLimitBlock(author);

        if (policyService.shouldReceiveNotification(author)) {
            notificationService.addCreationNotification(
                savedPost,
                existingPostCount,
                policyService.getMaxPostCountPerUser()
            );
        }

        eventPublisher.publishEvent(new PostCreatedEvent(savedPost.getId(), request.fileKeyList()));

        return PostCreateResponse.from(savedPost);
    }

    /**
     * 부스 계정 게시글 생성
     */
    private PostCreateResponse createBoothPost(PostCreateRequest request, String authorId) {
        policyService.validateBoothPostLimit(authorId);

        String s2TokenId = locationService.generateS2TokenId(request.latitude(), request.longitude());
        Post boothPost = Post.createNonExpiredStandalone(
            Location.create(request.latitude(), request.longitude()),
            s2TokenId,
            request.content(),
            authorId,
            false,
            null,
            null
        );

        Post savedBoothPost = postRepository.save(boothPost);
        eventPublisher.publishEvent(
            new PostCreatedEvent(savedBoothPost.getId(), request.fileKeyList()));

        return PostCreateResponse.from(savedBoothPost);
    }

    /**
     * Cloud 타입에 따라 게시글 생성
     */
    private Post createPostBasedOnCloudType(
        PostCreationCommand command,
        Optional<StaticCloud> staticCloud
    ) {
        if (staticCloud.isPresent()) {
            return createPostInStaticCloud(command, staticCloud.get());
        } else {
            // 정적 구름이 없으면 무조건 독립 게시물(알갱이)로 생성
            return createStandalonePost(command);
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

    private Post createStandalonePost(PostCreationCommand command) {
        return Post.createStandalone(
            command.location(),
            command.s2TokenId(),
            command.content(),
            command.authorId(),
            command.isAnonymous()
        );
    }
}
