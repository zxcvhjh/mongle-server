package com.algangi.mongle.map.presentation.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.algangi.mongle.auth.infrastructure.security.authentication.CustomUserDetails;
import com.algangi.mongle.map.application.service.MapQueryService;
import com.algangi.mongle.map.presentation.dto.MapObjectsRequest;
import com.algangi.mongle.map.presentation.dto.MapObjectsResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * 지도 API 컨트롤러
 * ApiResponseAdvice에 의해 자동으로 ApiResponse로 래핑됨
 */
@RestController
@RequestMapping("/api/v1/map")
@RequiredArgsConstructor
public class MapController {

    private final MapQueryService mapQueryService;

    @GetMapping("/objects")
    public MapObjectsResponse getMapObjects(
        @Valid @ModelAttribute MapObjectsRequest request,
        @AuthenticationPrincipal CustomUserDetails user) {
        String memberId = (user != null) ? user.userId() : null;
        return mapQueryService.getMapObjects(request, memberId);
    }
}
