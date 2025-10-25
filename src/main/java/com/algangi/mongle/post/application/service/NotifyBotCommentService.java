package com.algangi.mongle.post.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.algangi.mongle.comment.domain.model.Comment;
import com.algangi.mongle.comment.domain.repository.CommentRepository;
import com.algangi.mongle.member.application.service.MemberFinder;
import com.algangi.mongle.member.domain.model.Member;
import com.algangi.mongle.post.domain.model.Post;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class NotifyBotCommentService {

    private static final String NOTIFY_BOT_ID = "admin_notify_bot";
    private final MemberFinder memberFinder;
    private final CommentRepository commentRepository;

    @Transactional
    public void notifyByComment(String content, Post targetPost) {
        Member admin = memberFinder.getMemberOrThrow(NOTIFY_BOT_ID);
        Comment noifyComment = Comment.createParentComment(content, targetPost, admin,
            false);
        commentRepository.save(noifyComment);
    }

}
