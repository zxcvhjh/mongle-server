package com.algangi.mongle.post.application.service;

import com.algangi.mongle.global.util.AuthorizationUtil;
import com.algangi.mongle.member.application.service.MemberFinder;
import com.algangi.mongle.member.domain.model.Member;
import com.algangi.mongle.post.application.helper.PostFinder;
import com.algangi.mongle.post.domain.model.Post;
import com.algangi.mongle.post.event.PostDeletedEvent;
import com.algangi.mongle.post.exception.PostErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class PostCommandService {

    private final PostFinder postFinder;
    private final MemberFinder memberFinder;
    private final ApplicationEventPublisher eventPublisher;

    public void deletePost(String postId, String memberId) {
        Member member = memberFinder.getMemberOrThrow(memberId);
        Post post = postFinder.getPostOrThrow(postId);

        // 작성자 또는 관리자만 삭제 가능
        AuthorizationUtil.validateOwnershipOrAdmin(
            post.getAuthorId(),
            member,
            PostErrorCode.POST_ACCESS_DENIED
        );

        post.softDeleteByUser();
        eventPublisher.publishEvent(new PostDeletedEvent(postId));
    }
}