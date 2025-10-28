package com.algangi.mongle.post.presentation.controller;

import com.algangi.mongle.auth.infrastructure.security.authentication.CustomUserDetails;
import com.algangi.mongle.global.dto.ApiResponse;
import com.algangi.mongle.post.application.service.AdminPostService;
import com.algangi.mongle.post.presentation.dto.AdminPostCreateRequest;
import com.algangi.mongle.post.presentation.dto.AdminPostUpdateRequest;
import com.algangi.mongle.post.presentation.dto.PostCreateResponse;
import com.algangi.mongle.post.presentation.dto.PostUpdateResponse;
import com.algangi.mongle.post.presentation.dto.UpdateInfoTextRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/posts")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminPostController {

    private final AdminPostService adminPostService;

    @PostMapping
    public ResponseEntity<ApiResponse<PostCreateResponse>> createAdminPost(
        @Valid @RequestBody AdminPostCreateRequest request,
        @AuthenticationPrincipal CustomUserDetails user) {
        PostCreateResponse response = adminPostService.createAdminPost(request, user.userId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/{postId}")
    public ResponseEntity<ApiResponse<PostUpdateResponse>> updateAdminPost(
        @PathVariable String postId,
        @Valid @RequestBody AdminPostUpdateRequest request,
        @AuthenticationPrincipal CustomUserDetails user) {
        PostUpdateResponse response = adminPostService.updateAdminPost(postId, request,
            user.userId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @DeleteMapping("/{postId}")
    public ResponseEntity<ApiResponse<Void>> deleteAdminPost(
        @PathVariable String postId,
        @AuthenticationPrincipal CustomUserDetails user) {
        adminPostService.deleteAdminPost(postId, user.userId());
        return ResponseEntity.ok(ApiResponse.success());
    }

    @PatchMapping("/{postId}/info-text")
    public ResponseEntity<ApiResponse<Void>> updatePostInfoText(
        @PathVariable String postId,
        @Valid @RequestBody UpdateInfoTextRequest request,
        @AuthenticationPrincipal CustomUserDetails user) {

        adminPostService.updatePostInfoText(postId, request, user.userId());
        return ResponseEntity.ok(ApiResponse.success());
    }
}

