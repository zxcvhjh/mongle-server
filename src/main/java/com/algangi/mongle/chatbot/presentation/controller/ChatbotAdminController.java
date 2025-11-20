package com.algangi.mongle.chatbot.presentation.controller;

import com.algangi.mongle.auth.application.service.authentication.AccessTokenManager;
import com.algangi.mongle.chatbot.application.dto.ChatbotLogStatistics;
import com.algangi.mongle.chatbot.application.service.ChatbotAdminService;
import com.algangi.mongle.chatbot.domain.model.ChatbotQueryLog;
import com.algangi.mongle.global.exception.ApplicationException;
import com.algangi.mongle.global.exception.ErrorCode;
import com.algangi.mongle.member.domain.model.Member;
import com.algangi.mongle.member.domain.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 챗봇 관리자 컨트롤러
 * URL 쿼리 파라미터로 토큰을 받아서 ADMIN 권한 검증
 */
@Slf4j
@Controller
@RequestMapping("/chatbot/admin")
@RequiredArgsConstructor
public class ChatbotAdminController {

    private final ChatbotAdminService adminService;
    private final AccessTokenManager accessTokenManager;
    private final MemberRepository memberRepository;

    /**
     * 챗봇 로그 관리 대시보드
     * URL 쿼리 파라미터로 JWT 토큰을 받아서 ADMIN 권한 검증
     *
     * @param token JWT Access Token (필수)
     * @param page 페이지 번호 (0부터 시작)
     * @param size 페이지 크기
     * @param filter 필터 (all, success, failed)
     * @param model 뷰 모델
     * @return 대시보드 뷰
     */
    @GetMapping
    public String adminDashboard(
        @RequestParam(required = true) String token,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        @RequestParam(defaultValue = "all") String filter,
        Model model
    ) {
        // 1. 토큰 검증 (유효하지 않으면 예외 발생)
        try {
            accessTokenManager.validateToken(token);
        } catch (Exception e) {
            log.warn("유효하지 않은 토큰으로 관리자 페이지 접근 시도");
            throw new ApplicationException(new ErrorCode() {
                @Override
                public HttpStatus getHttpStatus() {
                    return HttpStatus.UNAUTHORIZED;
                }

                @Override
                public String getCode() {
                    return "UNAUTHORIZED";
                }

                @Override
                public String getMessage() {
                    return "유효하지 않은 인증 토큰입니다.";
                }
            });
        }

        // 2. 토큰에서 사용자 ID 추출
        String memberId = accessTokenManager.getUserId(token);

        // 3. 사용자 정보 조회
        Member member = memberRepository.findById(memberId)
            .orElseThrow(() -> new ApplicationException(new ErrorCode() {
                @Override
                public HttpStatus getHttpStatus() {
                    return HttpStatus.FORBIDDEN;
                }

                @Override
                public String getCode() {
                    return "MEMBER_NOT_FOUND";
                }

                @Override
                public String getMessage() {
                    return "사용자를 찾을 수 없습니다.";
                }
            }));

        // 4. ADMIN 권한 검증
        if (!member.isAdmin()) {
            log.warn("ADMIN 권한 없는 사용자의 관리자 페이지 접근 시도: memberId={}", memberId);
            throw new ApplicationException(new ErrorCode() {
                @Override
                public HttpStatus getHttpStatus() {
                    return HttpStatus.FORBIDDEN;
                }

                @Override
                public String getCode() {
                    return "FORBIDDEN";
                }

                @Override
                public String getMessage() {
                    return "관리자 권한이 필요합니다.";
                }
            });
        }

        log.info("챗봇 관리자 대시보드 접근: memberId={}, page={}, size={}, filter={}", memberId, page, size, filter);

        // 5. 최신순으로 정렬
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdDate"));

        // 6. 필터에 따라 로그 조회
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

        // 7. 통계 조회
        ChatbotLogStatistics statistics = adminService.getStatistics();

        // 8. 모델에 데이터 추가
        model.addAttribute("logs", logs);
        model.addAttribute("statistics", statistics);
        model.addAttribute("currentFilter", filter);
        model.addAttribute("currentPage", page);
        model.addAttribute("pageSize", size);
        model.addAttribute("token", token);  // 템플릿에서 링크에 토큰 유지

        return "chatbot/admin/dashboard";
    }
}
