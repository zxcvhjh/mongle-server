package com.algangi.mongle.post.presentation.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.algangi.mongle.auth.infrastructure.security.authentication.CustomUserDetails;
import com.algangi.mongle.post.application.service.PostCommandService;
import com.algangi.mongle.post.application.service.PostCreationService;
import com.algangi.mongle.post.application.service.PostQueryService;
import com.algangi.mongle.post.application.service.PostUpdateService;
import com.algangi.mongle.post.presentation.dto.PostCreateRequest;
import com.algangi.mongle.post.presentation.dto.PostCreateResponse;
import com.algangi.mongle.post.presentation.dto.PostDetailResponse;
import com.algangi.mongle.post.presentation.dto.PostListRequest;
import com.algangi.mongle.post.presentation.dto.PostListResponse;
import com.algangi.mongle.post.presentation.dto.PostStatsResponse;
import com.algangi.mongle.post.presentation.dto.PostUpdateRequest;
import com.algangi.mongle.post.presentation.dto.PostUpdateResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * 게시글 API 컨트롤러
 * ApiResponseAdvice에 의해 자동으로 ApiResponse로 래핑됨
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class PostController {

    private final PostCreationService postCreationService;
    private final PostQueryService postQueryService;
    private final PostCommandService postCommandService;
    private final PostUpdateService postUpdateService;

    @PostMapping("/posts")
    public PostCreateResponse createPost(
        @Valid @RequestBody PostCreateRequest dto,
        @AuthenticationPrincipal CustomUserDetails user) {
        return postCreationService.createPost(dto, user.userId());
    }

    @GetMapping("/posts")
    public PostListResponse getPostList(
        @Valid @ModelAttribute PostListRequest request,
        @AuthenticationPrincipal CustomUserDetails user) {
        String memberId = (user != null) ? user.userId() : null;
        return postQueryService.getPostList(request, memberId);
    }

    @GetMapping("/posts/{postId}")
    public PostDetailResponse getPostDetail(
        @PathVariable String postId,
        @AuthenticationPrincipal CustomUserDetails user) {
        String memberId = (user != null) ? user.userId() : null;
        return postQueryService.getPostDetail(postId, memberId);
    }

    @GetMapping("/posts/{postId}/stats")
    public PostStatsResponse getPostStats(
        @PathVariable String postId) {
        return postQueryService.getPostStats(postId);
    }

    @DeleteMapping("/posts/{postId}")
    public void deletePost(
        @PathVariable String postId,
        @AuthenticationPrincipal CustomUserDetails user
    ) {
        postCommandService.deletePost(postId, user.userId());
    }

    @PutMapping("/posts/{postId}")
    public PostUpdateResponse updatePost(
        @PathVariable(name = "postId") String postId,
        @Valid @RequestBody PostUpdateRequest request,
        @AuthenticationPrincipal CustomUserDetails user) {
        return postUpdateService.updatePost(postId, request, user.userId());
    }
}

