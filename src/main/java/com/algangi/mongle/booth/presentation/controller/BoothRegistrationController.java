package com.algangi.mongle.booth.presentation.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.algangi.mongle.booth.application.service.BoothRegistrationService;
import com.algangi.mongle.booth.presentation.dto.BoothRegistrationRequest;
import com.algangi.mongle.booth.presentation.dto.BoothRegistrationResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * 부스 등록 API 컨트롤러
 * ApiResponseAdvice에 의해 자동으로 ApiResponse로 래핑됨
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/booths")
public class BoothRegistrationController {

    private final BoothRegistrationService boothRegistrationService;

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public BoothRegistrationResponse registerBooth(
        @RequestBody @Valid
        BoothRegistrationRequest request) {
        return boothRegistrationService.registerBooth(request);
    }

}
