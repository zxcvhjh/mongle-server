package com.algangi.mongle.post.presentation.controller;

import com.algangi.mongle.auth.infrastructure.security.authentication.CustomUserDetails;
import com.algangi.mongle.post.application.service.AdminPostService;
import com.algangi.mongle.post.presentation.dto.AdminPostCreateRequest;
import com.algangi.mongle.post.presentation.dto.AdminPostUpdateRequest;
import com.algangi.mongle.post.presentation.dto.PostCreateResponse;
import com.algangi.mongle.post.presentation.dto.PostUpdateResponse;
import com.algangi.mongle.post.presentation.dto.UpdateInfoTextRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 관리자 게시글 API 컨트롤러
 * ApiResponseAdvice에 의해 자동으로 ApiResponse로 래핑됨
 */
@RestController
@RequestMapping("/api/v1/admin/posts")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminPostController {

    private final AdminPostService adminPostService;

    @PostMapping
    public PostCreateResponse createAdminPost(
        @Valid @RequestBody AdminPostCreateRequest request,
        @AuthenticationPrincipal CustomUserDetails user) {
        return adminPostService.createAdminPost(request, user.userId());
    }

    @PutMapping("/{postId}")
    public PostUpdateResponse updateAdminPost(
        @PathVariable String postId,
        @Valid @RequestBody AdminPostUpdateRequest request,
        @AuthenticationPrincipal CustomUserDetails user) {
        return adminPostService.updateAdminPost(postId, request, user.userId());
    }

    @DeleteMapping("/{postId}")
    public void deleteAdminPost(
        @PathVariable String postId,
        @AuthenticationPrincipal CustomUserDetails user) {
        adminPostService.deleteAdminPost(postId, user.userId());
    }

    @PatchMapping("/{postId}/info-text")
    public void updatePostInfoText(
        @PathVariable String postId,
        @Valid @RequestBody UpdateInfoTextRequest request,
        @AuthenticationPrincipal CustomUserDetails user) {

        adminPostService.updatePostInfoText(postId, request, user.userId());
    }
}

