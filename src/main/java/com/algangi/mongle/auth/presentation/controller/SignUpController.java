package com.algangi.mongle.auth.presentation.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.algangi.mongle.auth.application.service.authentication.SignUpService;
import com.algangi.mongle.auth.application.service.email.EmailVerificationService;
import com.algangi.mongle.auth.presentation.dto.SendVerificationCodeRequest;
import com.algangi.mongle.auth.presentation.dto.SignUpRequest;
import com.algangi.mongle.auth.presentation.dto.SignUpResponse;
import com.algangi.mongle.auth.presentation.dto.VerifyEmailRequest;
import com.algangi.mongle.auth.presentation.dto.VerifyEmailResponse;
import com.algangi.mongle.auth.presentation.dto.VerifyNicknameResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * 회원가입 API 컨트롤러
 * ApiResponseAdvice에 의해 자동으로 ApiResponse로 래핑됨
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class SignUpController {

    private final SignUpService signUpService;
    private final EmailVerificationService emailVerificationService;

    @PostMapping("/sign-up")
    public SignUpResponse signupMember(
        @Valid @RequestBody SignUpRequest request) {
        return signUpService.signUp(request);
    }

    @PostMapping("/verification-code")
    public void sendVerificationCode(
        @Valid @RequestBody SendVerificationCodeRequest request) {
        emailVerificationService.sendVerificationCode(request.email());
    }

    @PostMapping("/verify-code")
    public VerifyEmailResponse verifyVerificationCode(
        @Valid @RequestBody VerifyEmailRequest request
    ) {
        return emailVerificationService.verifyEmail(request);
    }

    @GetMapping("/verify-nickname")
    public VerifyNicknameResponse verifyNickname(
        @RequestParam("nickname") String nickname) {
        return signUpService.verifyNickname(nickname);
    }

}
