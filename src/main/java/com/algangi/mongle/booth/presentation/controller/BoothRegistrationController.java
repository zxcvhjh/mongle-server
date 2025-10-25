package com.algangi.mongle.booth.presentation.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.algangi.mongle.booth.application.service.BoothRegistrationService;
import com.algangi.mongle.booth.presentation.dto.BoothRegistrationRequest;
import com.algangi.mongle.booth.presentation.dto.BoothRegistrationResponse;
import com.algangi.mongle.global.dto.ApiResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/booths")
public class BoothRegistrationController {

    private final BoothRegistrationService boothRegistrationService;

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<ApiResponse<BoothRegistrationResponse>> registerBooth(
        @RequestBody @Valid
        BoothRegistrationRequest request) {
        return ResponseEntity.ok(
            ApiResponse.success(boothRegistrationService.registerBooth(request)));
    }

}
