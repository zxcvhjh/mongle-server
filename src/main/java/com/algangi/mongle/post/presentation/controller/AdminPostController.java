package com.algangi.mongle.post.presentation.controller;

import com.algangi.mongle.auth.infrastructure.security.authentication.CustomUserDetails;
import com.algangi.mongle.global.dto.ApiResponse;
import com.algangi.mongle.post.application.service.PostCreationService;
import com.algangi.mongle.post.presentation.dto.AdminPostCreateRequest;
import com.algangi.mongle.post.presentation.dto.PostCreateResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/posts")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminPostController {

    private final PostCreationService postCreationService;

    @PostMapping
    public ResponseEntity<ApiResponse<PostCreateResponse>> createAdminPost(
        @Valid @RequestBody AdminPostCreateRequest request,
        @AuthenticationPrincipal CustomUserDetails user) {

        PostCreateResponse response = postCreationService.createAdminPost(request, user.userId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

}

