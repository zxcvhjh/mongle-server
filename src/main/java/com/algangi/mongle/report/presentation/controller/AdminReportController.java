package com.algangi.mongle.report.presentation.controller;

import com.algangi.mongle.report.application.service.ReportCommandService;
import com.algangi.mongle.report.application.service.ReportQueryService;
import com.algangi.mongle.report.domain.model.ReportStatus;
import com.algangi.mongle.report.presentation.dto.ReportAdminResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 관리자 신고 API 컨트롤러
 * ApiResponseAdvice에 의해 자동으로 ApiResponse로 래핑됨
 */
@RestController
@RequestMapping("/api/v1/admin/reports")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminReportController {

    private final ReportQueryService reportQueryService;
    private final ReportCommandService reportCommandService;

    @GetMapping
    public Page<ReportAdminResponse> getReportList(
        @PageableDefault(size = 20) Pageable pageable) {
        return reportQueryService.getReportList(pageable);
    }

    @PatchMapping("/{reportId}/status")
    public void updateReportStatus(
        @PathVariable String reportId,
        @RequestParam ReportStatus status) {
        reportCommandService.updateReportStatus(reportId, status);
    }
}
