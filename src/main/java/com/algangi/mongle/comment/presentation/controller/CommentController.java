package com.algangi.mongle.comment.presentation.controller;

import com.algangi.mongle.auth.infrastructure.security.authentication.CustomUserDetails;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.algangi.mongle.comment.application.service.CommentCommandService;
import com.algangi.mongle.comment.application.service.CommentQueryService;
import com.algangi.mongle.comment.presentation.cursor.CursorInfoResponse;
import com.algangi.mongle.comment.presentation.dto.CommentCreateRequest;
import com.algangi.mongle.comment.presentation.dto.CommentInfoResponse;
import com.algangi.mongle.comment.presentation.dto.CommentQueryRequest;
import com.algangi.mongle.comment.presentation.mapper.CommentRequestMapper;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * 댓글 API 컨트롤러
 * ApiResponseAdvice에 의해 자동으로 ApiResponse로 래핑됨
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class CommentController {

    private final CommentCommandService commentCommandService;
    private final CommentQueryService commentQueryService;
    private final CommentRequestMapper commentRequestMapper;

    @GetMapping("/posts/{postId}/comments")
    public CursorInfoResponse<CommentInfoResponse> getCommentsByPost(
            @PathVariable(name = "postId") String postId,
            @ModelAttribute CommentQueryRequest request,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        String memberId = (user != null) ? user.userId() : null;
        var condition = commentRequestMapper.toPostCommentSearchCondition(postId, request);
        return commentQueryService.getCommentsByPost(condition, memberId, request.size());
    }

    @GetMapping("/comments/{parentCommentId}/replies")
    public CursorInfoResponse<CommentInfoResponse> getRepliesByParent(
            @PathVariable(name = "parentCommentId") String parentCommentId,
            @ModelAttribute CommentQueryRequest request,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        String memberId = (user != null) ? user.userId() : null;
        var condition = commentRequestMapper.toReplySearchCondition(parentCommentId, request);
        return commentQueryService.getRepliesByParent(condition, memberId, request.size());
    }

    @PostMapping("/posts/{postId}/comments")
    public void createParentComment(
            @PathVariable(name = "postId") String postId,
            @Valid @RequestBody CommentCreateRequest dto,
            @AuthenticationPrincipal CustomUserDetails user)  {
        commentCommandService.createParentComment(postId, dto, user.userId());
    }

    @PostMapping("/comments/{parentCommentId}/replies")
    public void createChildComment(
            @PathVariable(name = "parentCommentId") String parentCommentId,
            @Valid @RequestBody CommentCreateRequest dto,
            @AuthenticationPrincipal CustomUserDetails user) {
        commentCommandService.createChildComment(parentCommentId, dto, user.userId());
    }

    @DeleteMapping("/comments/{commentId}")
    public void deleteComment(
            @PathVariable(name = "commentId") String commentId,
            @AuthenticationPrincipal CustomUserDetails user) {
        commentCommandService.deleteComment(commentId, user.userId());
    }

}