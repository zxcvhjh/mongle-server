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

import java.time.*;
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
    public Page<ChatbotQueryLog> getLogsByDateRange(Instant startDate, Instant endDate, Pageable pageable) {
        return queryLogRepository.findByCreatedDateBetween(startDate, endDate, pageable);
    }

    /**
     * 챗봇 로그 통계 조회
     *
     * @return 통계 정보
     */
    public ChatbotLogStatistics getStatistics() {
        ZoneId zoneId = ZoneId.systemDefault();

        // 전체 통계
        long totalQuestions = queryLogRepository.count();
        long successfulQuestions = queryLogRepository.countByIsSuccess(true);
        long failedQuestions = queryLogRepository.countByIsSuccess(false);
        double successRate = totalQuestions > 0
            ? (double) successfulQuestions / totalQuestions * 100
            : 0.0;

        // 오늘 통계
        Instant startOfToday = LocalDate.now().atStartOfDay(zoneId).toInstant();
        Instant endOfToday = LocalDate.now().atTime(LocalTime.MAX).atZone(zoneId).toInstant();
        long todayQuestions = queryLogRepository.countByCreatedDateBetween(startOfToday, endOfToday);

        // 이번 주 통계 (월요일 시작)
        Instant startOfWeek = LocalDate.now()
            .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            .atStartOfDay(zoneId)
            .toInstant();
        Instant endOfWeek = Instant.now();
        long thisWeekQuestions = queryLogRepository.countByCreatedDateBetween(startOfWeek, endOfWeek);

        // 이번 달 통계
        Instant startOfMonth = LocalDate.now()
            .with(TemporalAdjusters.firstDayOfMonth())
            .atStartOfDay(zoneId)
            .toInstant();
        Instant endOfMonth = Instant.now();
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
