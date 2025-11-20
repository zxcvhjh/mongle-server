package com.algangi.mongle.chatbot.domain.repository;

import com.algangi.mongle.chatbot.domain.model.ChatbotQueryLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 챗봇 질문-답변 로그 Repository
 */
public interface ChatbotQueryLogRepository {

    /**
     * 로그 저장
     *
     * @param log 저장할 로그
     * @return 저장된 로그
     */
    ChatbotQueryLog save(ChatbotQueryLog log);

    /**
     * 모든 로그 조회 (페이징)
     *
     * @param pageable 페이징 정보
     * @return 로그 페이지
     */
    Page<ChatbotQueryLog> findAll(Pageable pageable);

    /**
     * 특정 기간의 로그 조회
     *
     * @param startDate 시작 날짜
     * @param endDate 종료 날짜
     * @param pageable 페이징 정보
     * @return 로그 페이지
     */
    Page<ChatbotQueryLog> findByCreatedDateBetween(LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);

    /**
     * 성공/실패 여부로 로그 조회
     *
     * @param isSuccess 성공 여부
     * @param pageable 페이징 정보
     * @return 로그 페이지
     */
    Page<ChatbotQueryLog> findByIsSuccess(Boolean isSuccess, Pageable pageable);

    /**
     * 전체 로그 수 조회
     *
     * @return 전체 로그 수
     */
    long count();

    /**
     * 성공한 로그 수 조회
     *
     * @return 성공한 로그 수
     */
    long countByIsSuccess(Boolean isSuccess);

    /**
     * 특정 기간의 로그 수 조회
     *
     * @param startDate 시작 날짜
     * @param endDate 종료 날짜
     * @return 로그 수
     */
    long countByCreatedDateBetween(LocalDateTime startDate, LocalDateTime endDate);

    /**
     * 최근 N개 로그 조회
     *
     * @param limit 조회 개수
     * @return 로그 리스트
     */
    List<ChatbotQueryLog> findRecentLogs(int limit);
}
