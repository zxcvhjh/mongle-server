package com.algangi.mongle.report.presentation.controller;

import com.algangi.mongle.auth.infrastructure.security.authentication.CustomUserDetails;
import com.algangi.mongle.report.application.service.ReportCommandService;
import com.algangi.mongle.report.presentation.dto.ReportCreateRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 신고 API 컨트롤러
 * ApiResponseAdvice에 의해 자동으로 ApiResponse로 래핑됨
 */
@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportCommandService reportCommandService;

    @PostMapping
    public void createReport(
        @AuthenticationPrincipal CustomUserDetails userDetails,
        @Valid @RequestBody ReportCreateRequest request) {

        String reporterId = (userDetails != null && userDetails.userId() != null
            && !userDetails.userId().equals("anonymousUser"))
            ? userDetails.userId()
            : null;

        reportCommandService.createReport(reporterId, request);
    }
}