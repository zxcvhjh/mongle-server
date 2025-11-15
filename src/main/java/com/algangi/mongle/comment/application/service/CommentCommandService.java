package com.algangi.mongle.comment.application.service;

import com.algangi.mongle.comment.presentation.dto.CommentCreateRequest;
import com.algangi.mongle.global.util.AuthorizationUtil;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.algangi.mongle.comment.application.event.CommentCreatedEvent;
import com.algangi.mongle.comment.application.event.CommentDeletedEvent;
import com.algangi.mongle.comment.domain.model.Comment;
import com.algangi.mongle.comment.domain.repository.CommentRepository;
import com.algangi.mongle.comment.domain.service.CommentDomainService;
import com.algangi.mongle.comment.domain.service.CommentFinder;
import com.algangi.mongle.comment.exception.CommentErrorCode;
import com.algangi.mongle.global.exception.ApplicationException;
import com.algangi.mongle.member.application.service.MemberFinder;
import com.algangi.mongle.member.domain.model.Member;
import com.algangi.mongle.post.application.helper.PostFinder;
import com.algangi.mongle.post.domain.model.Post;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CommentCommandService {

    private final MemberFinder memberFinder;
    private final PostFinder postFinder;
    private final CommentFinder commentFinder;
    private final CommentDomainService commentDomainService;
    private final CommentRepository commentRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public void createParentComment(String postId, CommentCreateRequest dto, String memberId) {
        Member author = memberFinder.getMemberOrThrow(memberId);
        author.validateActive();

        Post post = postFinder.getPostOrThrow(postId);

        boolean isAnonymous = isAnonymous(dto);

        Comment newComment = commentDomainService.createParentComment(post, author, dto.content(), isAnonymous);

        commentRepository.save(newComment);
        eventPublisher.publishEvent(new CommentCreatedEvent(postId, newComment.getId()));
    }

    @Transactional
    public void createChildComment(String parentCommentId, CommentCreateRequest dto, String memberId) {
        Member author = memberFinder.getMemberOrThrow(memberId);
        author.validateActive();

        Comment parent = commentFinder.getCommentOrThrow(parentCommentId);

        boolean isAnonymous = isAnonymous(dto);

        Comment newComment = commentDomainService.createChildComment(parent, author, dto.content(), isAnonymous);

        commentRepository.save(newComment);
        eventPublisher.publishEvent(
            new CommentCreatedEvent(parent.getPost().getId(), newComment.getId()));
    }

    @Transactional
    public void deleteComment(String commentId, String memberId) {
        Member member = memberFinder.getMemberOrThrow(memberId);
        member.validateActive();

        Comment comment = commentFinder.getCommentOrThrow(commentId);
        if (comment.isDeleted()) {
            throw new ApplicationException(CommentErrorCode.ALREADY_DELETED);
        }

        // 익명 댓글의 경우 member가 null이므로 AuthorizationUtil이 이를 처리
        // 작성자 또는 관리자만 삭제 가능
        String commentAuthorId = comment.getMember() != null ? comment.getMember().getMemberId() : null;
        AuthorizationUtil.validateOwnershipOrAdmin(
            commentAuthorId,
            member,
            CommentErrorCode.COMMENT_ACCESS_DENIED
        );

        commentDomainService.deleteComment(comment);
        eventPublisher.publishEvent(new CommentDeletedEvent(comment.getPost().getId()));
    }

    private boolean isAnonymous(CommentCreateRequest dto) {
        return Boolean.TRUE.equals(dto.isAnonymous());
    }
}