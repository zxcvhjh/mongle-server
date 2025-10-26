package com.algangi.mongle.post.application.service;

import com.algangi.mongle.global.domain.service.CellService;
import com.algangi.mongle.global.exception.ApplicationException;
import com.algangi.mongle.member.application.service.MemberFinder;
import com.algangi.mongle.member.domain.model.Member;
import com.algangi.mongle.post.application.helper.PostFinder;
import com.algangi.mongle.post.domain.model.Location;
import com.algangi.mongle.post.domain.model.Post;
import com.algangi.mongle.post.domain.model.PostFile;
import com.algangi.mongle.post.domain.repository.PostRepository;
import com.algangi.mongle.post.event.PostCreatedEvent;
import com.algangi.mongle.post.event.PostUpdatedEvent;
import com.algangi.mongle.post.exception.PostErrorCode;
import com.algangi.mongle.post.presentation.dto.AdminPostCreateRequest;
import com.algangi.mongle.post.presentation.dto.AdminPostUpdateRequest;
import com.algangi.mongle.post.presentation.dto.PostCreateResponse;
import com.algangi.mongle.post.presentation.dto.PostUpdateResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminPostService {

    private final PostRepository postRepository;
    private final MemberFinder memberFinder;
    private final CellService cellService;
    private final ApplicationEventPublisher eventPublisher;
    private final PostFinder postFinder;

    @Transactional
    public PostCreateResponse createAdminPost(AdminPostCreateRequest request, String authorId) {
        Member author = memberFinder.getMemberOrThrow(authorId);
        if (!author.isAdmin()) {
            log.warn("Non-admin user attempted to create admin post. userId={}", authorId);
            throw new ApplicationException(PostErrorCode.POST_ACCESS_DENIED);
        }

        Location location = Location.create(request.latitude(), request.longitude());
        String s2TokenId = cellService.generateS2TokenIdFrom(location.getLatitude(),
            location.getLongitude());

        Post adminPost = Post.createNonExpiredStandalone(
            location,
            s2TokenId,
            request.content(),
            authorId,
            request.isAnonymous() != null && request.isAnonymous(),
            request.infoText(),
            request.customNickname()
        );

        Post savedPost = postRepository.save(adminPost);

        eventPublisher.publishEvent(new PostCreatedEvent(savedPost.getId(), request.fileKeyList()));

        return PostCreateResponse.from(savedPost);
    }

    @Transactional
    public PostUpdateResponse updateAdminPost(String postId, AdminPostUpdateRequest request,
        String adminId) {
        Member admin = memberFinder.getMemberOrThrow(adminId);
        if (!admin.isAdmin()) {
            log.warn("Non-admin user attempted to update post. userId={}, postId={}", adminId,
                postId);
            throw new ApplicationException(PostErrorCode.POST_ACCESS_DENIED);
        }

        Post post = postFinder.getPostWithPessimisticLockOrThrow(postId);

        List<String> previousFileKeys = post.getPostFiles().stream()
            .map(PostFile::getFileKey)
            .collect(Collectors.toList());

        post.updateAdminDetails(
            request.content(),
            request.isAnonymous(),
            request.infoText(),
            request.customNickname()
        );

        List<String> finalFileKeys =
            request.fileKeyList() != null ? request.fileKeyList() : previousFileKeys;

        boolean filesChanged = !Objects.equals(previousFileKeys, finalFileKeys);

        if (filesChanged) {
            post.markAsPending();
            PostUpdatedEvent event = new PostUpdatedEvent(postId, previousFileKeys, finalFileKeys);
            eventPublisher.publishEvent(event);
        } else {
            post.markAsActive();
        }

        return PostUpdateResponse.of(post.getId(), post.getContent(), post.isAnonymous());
    }


    @Transactional
    public void deleteAdminPost(String postId, String adminId) {
        Member admin = memberFinder.getMemberOrThrow(adminId);
        if (!admin.isAdmin()) {
            log.warn("Non-admin user attempted to delete post. userId={}, postId={}", adminId,
                postId);
            throw new ApplicationException(PostErrorCode.POST_ACCESS_DENIED);
        }

        Post post = postFinder.getPostOrThrow(postId);
        post.softDeleteByAdmin();

    }
}

