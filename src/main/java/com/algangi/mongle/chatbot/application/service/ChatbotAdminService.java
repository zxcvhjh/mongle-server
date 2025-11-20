package com.algangi.mongle.chatbot.application.service;

import com.algangi.mongle.chatbot.application.dto.ChatbotLogStatistics;
import com.algangi.mongle.chatbot.domain.model.ChatbotQueryLog;
import com.algangi.mongle.chatbot.domain.repository.ChatbotQueryLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;

/**
 * 챗봇 관리자 서비스
 * 관리자 전용 로그 조회 및 통계 기능 제공
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatbotAdminService {

    private final ChatbotQueryLogRepository queryLogRepository;

    /**
     * 모든 챗봇 로그 조회 (페이징)
     *
     * @param pageable 페이징 정보
     * @return 로그 페이지
     */
    public Page<ChatbotQueryLog> getAllLogs(Pageable pageable) {
        return queryLogRepository.findAll(pageable);
    }

    /**
     * 성공/실패 여부로 로그 필터링 조회
     *
     * @param isSuccess 성공 여부
     * @param pageable 페이징 정보
     * @return 로그 페이지
     */
    public Page<ChatbotQueryLog> getLogsBySuccess(Boolean isSuccess, Pageable pageable) {
        return queryLogRepository.findByIsSuccess(isSuccess, pageable);
    }

    /**
     * 특정 기간의 로그 조회
     *
     * @param startDate 시작 날짜
     * @param endDate 종료 날짜
     * @param pageable 페이징 정보
     * @return 로그 페이지
     */
    public Page<ChatbotQueryLog> getLogsByDateRange(LocalDateTime startDate, LocalDateTime endDate, Pageable pageable) {
        return queryLogRepository.findByCreatedDateBetween(startDate, endDate, pageable);
    }

    /**
     * 챗봇 로그 통계 조회
     *
     * @return 통계 정보
     */
    public ChatbotLogStatistics getStatistics() {
        // 전체 통계
        long totalQuestions = queryLogRepository.count();
        long successfulQuestions = queryLogRepository.countByIsSuccess(true);
        long failedQuestions = queryLogRepository.countByIsSuccess(false);
        double successRate = totalQuestions > 0
            ? (double) successfulQuestions / totalQuestions * 100
            : 0.0;

        // 오늘 통계
        LocalDateTime startOfToday = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
        LocalDateTime endOfToday = LocalDateTime.of(LocalDate.now(), LocalTime.MAX);
        long todayQuestions = queryLogRepository.countByCreatedDateBetween(startOfToday, endOfToday);

        // 이번 주 통계 (월요일 시작)
        LocalDateTime startOfWeek = LocalDateTime.of(
            LocalDate.now().with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY)),
            LocalTime.MIN
        );
        LocalDateTime endOfWeek = LocalDateTime.now();
        long thisWeekQuestions = queryLogRepository.countByCreatedDateBetween(startOfWeek, endOfWeek);

        // 이번 달 통계
        LocalDateTime startOfMonth = LocalDateTime.of(
            LocalDate.now().with(TemporalAdjusters.firstDayOfMonth()),
            LocalTime.MIN
        );
        LocalDateTime endOfMonth = LocalDateTime.now();
        long thisMonthQuestions = queryLogRepository.countByCreatedDateBetween(startOfMonth, endOfMonth);

        return ChatbotLogStatistics.builder()
            .totalQuestions(totalQuestions)
            .successfulQuestions(successfulQuestions)
            .failedQuestions(failedQuestions)
            .successRate(Math.round(successRate * 100.0) / 100.0) // 소수점 2자리
            .todayQuestions(todayQuestions)
            .thisWeekQuestions(thisWeekQuestions)
            .thisMonthQuestions(thisMonthQuestions)
            .build();
    }
}
