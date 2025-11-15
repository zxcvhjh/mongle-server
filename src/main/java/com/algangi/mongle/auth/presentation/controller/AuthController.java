package com.algangi.mongle.auth.presentation.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.algangi.mongle.auth.application.service.authentication.LoginService;
import com.algangi.mongle.auth.application.service.authentication.TokenReissueService;
import com.algangi.mongle.auth.presentation.dto.LoginRequest;
import com.algangi.mongle.auth.presentation.dto.ReissueTokenRequest;
import com.algangi.mongle.auth.presentation.dto.TokenInfo;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * 인증 API 컨트롤러
 * ApiResponseAdvice에 의해 자동으로 ApiResponse로 래핑됨
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final LoginService loginService;
    private final TokenReissueService tokenReissueService;

    @PostMapping("/login")
    public TokenInfo loginMember(
        @Valid @RequestBody LoginRequest request) {
        return loginService.login(request);
    }

    @PostMapping("/reissue")
    public TokenInfo reissueTokens(
        @Valid @RequestBody ReissueTokenRequest reissueTokenRequest) {
        return tokenReissueService.reissueTokens(reissueTokenRequest);
    }
}
