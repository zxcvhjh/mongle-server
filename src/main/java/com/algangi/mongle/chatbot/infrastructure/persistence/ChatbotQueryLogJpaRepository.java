package com.algangi.mongle.chatbot.infrastructure.persistence;

import com.algangi.mongle.chatbot.domain.model.ChatbotQueryLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

/**
 * 챗봇 질문-답변 로그 JPA Repository
 */
public interface ChatbotQueryLogJpaRepository extends JpaRepository<ChatbotQueryLog, String> {

    /**
     * 특정 기간의 로그 조회
     */
    Page<ChatbotQueryLog> findByCreatedDateBetween(Instant startDate, Instant endDate, Pageable pageable);

    /**
     * 성공/실패 여부로 로그 조회
     */
    Page<ChatbotQueryLog> findByIsSuccess(Boolean isSuccess, Pageable pageable);

    /**
     * 성공/실패 로그 수 조회
     */
    long countByIsSuccess(Boolean isSuccess);

    /**
     * 특정 기간의 로그 수 조회
     */
    long countByCreatedDateBetween(Instant startDate, Instant endDate);

    /**
     * 최근 N개 로그 조회
     */
    @Query("SELECT l FROM ChatbotQueryLog l ORDER BY l.createdDate DESC LIMIT :limit")
    List<ChatbotQueryLog> findRecentLogs(@Param("limit") int limit);
}
