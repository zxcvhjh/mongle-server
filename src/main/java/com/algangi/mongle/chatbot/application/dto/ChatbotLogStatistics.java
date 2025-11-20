package com.algangi.mongle.chatbot.application.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * 챗봇 로그 통계 정보
 */
@Getter
@Builder
public class ChatbotLogStatistics {

    /**
     * 전체 질문 수
     */
    private long totalQuestions;

    /**
     * 성공한 질문 수
     */
    private long successfulQuestions;

    /**
     * 실패한 질문 수
     */
    private long failedQuestions;

    /**
     * 성공률 (%)
     */
    private double successRate;

    /**
     * 오늘의 질문 수
     */
    private long todayQuestions;

    /**
     * 이번 주 질문 수
     */
    private long thisWeekQuestions;

    /**
     * 이번 달 질문 수
     */
    private long thisMonthQuestions;
}
