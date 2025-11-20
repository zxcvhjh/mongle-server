package com.algangi.mongle.chatbot.presentation.controller;

import com.algangi.mongle.chatbot.application.dto.ChatbotLogStatistics;
import com.algangi.mongle.chatbot.application.service.ChatbotAdminService;
import com.algangi.mongle.chatbot.domain.model.ChatbotQueryLog;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 챗봇 관리자 컨트롤러
 * 관리자 전용 로그 조회 페이지 제공
 */
@Slf4j
@Controller
@RequestMapping("/chatbot/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class ChatbotAdminController {

    private final ChatbotAdminService adminService;

    /**
     * 챗봇 로그 관리 대시보드
     *
     * @param page 페이지 번호 (0부터 시작)
     * @param size 페이지 크기
     * @param filter 필터 (all, success, failed)
     * @param model 뷰 모델
     * @return 대시보드 뷰
     */
    @GetMapping
    public String adminDashboard(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        @RequestParam(defaultValue = "all") String filter,
        Model model
    ) {
        log.info("챗봇 관리자 대시보드 접근: page={}, size={}, filter={}", page, size, filter);

        // 최신순으로 정렬
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdDate"));

        // 필터에 따라 로그 조회
        Page<ChatbotQueryLog> logs;
        switch (filter) {
            case "success":
                logs = adminService.getLogsBySuccess(true, pageable);
                break;
            case "failed":
                logs = adminService.getLogsBySuccess(false, pageable);
                break;
            default:
                logs = adminService.getAllLogs(pageable);
        }

        // 통계 조회
        ChatbotLogStatistics statistics = adminService.getStatistics();

        // 모델에 데이터 추가
        model.addAttribute("logs", logs);
        model.addAttribute("statistics", statistics);
        model.addAttribute("currentFilter", filter);
        model.addAttribute("currentPage", page);
        model.addAttribute("pageSize", size);

        return "chatbot/admin/dashboard";
    }
}
