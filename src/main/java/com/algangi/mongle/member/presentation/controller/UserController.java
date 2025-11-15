package com.algangi.mongle.member.presentation.controller;

import com.algangi.mongle.member.presentation.dto.UpdateNicknameRequest;
import com.algangi.mongle.member.presentation.dto.UpdateNicknameResponse;
import com.algangi.mongle.member.presentation.dto.UpdateProfileImageRequest;
import com.algangi.mongle.member.presentation.dto.UpdateProfileImageResponse;
import jakarta.validation.Valid;
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
import com.algangi.mongle.member.application.service.MemberProfileService;
import com.algangi.mongle.member.application.service.MemberService;
import com.algangi.mongle.member.presentation.dto.UserDetailResponse;

import lombok.RequiredArgsConstructor;

/**
 * 사용자 API 컨트롤러
 * ApiResponseAdvice에 의해 자동으로 ApiResponse로 래핑됨
 */
@RestController
@RequestMapping("/api/v1/user/me")
@RequiredArgsConstructor
public class UserController {

    private final OAuth2Service oAuth2Service;
    private final MemberProfileService memberProfileService;
    private final MemberService memberService;

    @PostMapping("/social-link/{registrationId}")
    public void linkSocialAccount(
        @AuthenticationPrincipal CustomUserDetails userDetails,
        @PathVariable(name = "registrationId") String registrationId,
        @RequestParam(name = "code") String authorizationCode
    ) {
        oAuth2Service.linkSocialAccount(userDetails.userId(), registrationId, authorizationCode);
    }

    @GetMapping
    public UserDetailResponse getUserDetails(
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return memberProfileService.getUserDetails(userDetails.userId());
    }

    @DeleteMapping
    public void withdrawUser(
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        memberService.withdrawMember(userDetails.userId());
    }

    @PutMapping("/profile-image")
    public UpdateProfileImageResponse updateProfileImage(
        @AuthenticationPrincipal CustomUserDetails userDetails,
        @Valid @RequestBody UpdateProfileImageRequest request
    ) {
        String userId = userDetails.userId();
        return memberProfileService.updateProfileImage(userId, request.fileKey());
    }

    @PutMapping("/nickname")
    public UpdateNicknameResponse updateNickname(
        @AuthenticationPrincipal CustomUserDetails userDetails,
        @Valid @RequestBody UpdateNicknameRequest request
    ) {
        String userId = userDetails.userId();
        return memberProfileService.updateNickname(userId, request.nickname());
    }
}
