package com.algangi.mongle.member.presentation.controller;

import com.algangi.mongle.member.presentation.dto.UpdateNicknameRequest;
import com.algangi.mongle.member.presentation.dto.UpdateNicknameResponse;
import com.algangi.mongle.member.presentation.dto.UpdateProfileImageRequest;
import com.algangi.mongle.member.presentation.dto.UpdateProfileImageResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.algangi.mongle.auth.application.service.oauth.OAuth2Service;
import com.algangi.mongle.auth.infrastructure.security.authentication.CustomUserDetails;
import com.algangi.mongle.global.dto.ApiResponse;
import com.algangi.mongle.member.application.service.MemberProfileService;
import com.algangi.mongle.member.application.service.MemberService;
import com.algangi.mongle.member.presentation.dto.UserDetailResponse;

import lombok.RequiredArgsConstructor;


@RestController
@RequestMapping("/api/v1/user/me")
@RequiredArgsConstructor
public class UserController {

    private final OAuth2Service oAuth2Service;
    private final MemberProfileService memberProfileService;
    private final MemberService memberService;

    @PostMapping("/social-link/{registrationId}")
    public ResponseEntity<ApiResponse<Void>> linkSocialAccount(
        @AuthenticationPrincipal CustomUserDetails userDetails,
        @PathVariable(name = "registrationId") String registrationId,
        @RequestParam(name = "code") String authorizationCode
    ) {
        oAuth2Service.linkSocialAccount(userDetails.userId(), registrationId, authorizationCode);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<UserDetailResponse>> getUserDetails(
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ResponseEntity.ok(
            ApiResponse.success(memberProfileService.getUserDetails(userDetails.userId())));
    }

    @DeleteMapping
    public ResponseEntity<ApiResponse<Void>> withdrawUser(
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        memberService.withdrawMember(userDetails.userId());
        return ResponseEntity.ok(ApiResponse.success());
    }

    @PutMapping("/profile-image")
    public ResponseEntity<ApiResponse<UpdateProfileImageResponse>> updateProfileImage(
        @AuthenticationPrincipal CustomUserDetails userDetails,
        @Valid @RequestBody UpdateProfileImageRequest request
    ) {
        String userId = userDetails.userId();
        UpdateProfileImageResponse response = memberProfileService.updateProfileImage(userId,
            request.fileKey());
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    @PutMapping("/nickname")
    public ResponseEntity<ApiResponse<UpdateNicknameResponse>> updateNickname(
        @AuthenticationPrincipal CustomUserDetails userDetails,
        @Valid @RequestBody UpdateNicknameRequest request
    ) {
        String userId = userDetails.userId();
        UpdateNicknameResponse response = memberProfileService.updateNickname(userId,
            request.nickname());
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
