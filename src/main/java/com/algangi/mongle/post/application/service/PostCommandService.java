package com.algangi.mongle.post.application.service;

import com.algangi.mongle.global.exception.ApplicationException;
import com.algangi.mongle.member.application.service.MemberFinder;
import com.algangi.mongle.member.domain.model.Member;
import com.algangi.mongle.member.domain.model.MemberRole;
import com.algangi.mongle.post.application.helper.PostFinder;
import com.algangi.mongle.post.domain.model.Post;
import com.algangi.mongle.post.exception.PostErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
@RequiredArgsConstructor
@Transactional
public class PostCommandService {

    private final PostFinder postFinder;
    private final MemberFinder memberFinder;

    public void deletePost(String postId, String memberId) {
        Member member = memberFinder.getMemberOrThrow(memberId);
        Post post = postFinder.getPostOrThrow(postId);

        boolean isAuthor = Objects.equals(post.getAuthorId(), member.getMemberId()); // 또는 memberId
        boolean isAdmin = member.getMemberRole() == MemberRole.ADMIN;

        if (!isAuthor && !isAdmin) {
            throw new ApplicationException(PostErrorCode.POST_ACCESS_DENIED);
        }

        post.softDeleteByUser();
    }
}