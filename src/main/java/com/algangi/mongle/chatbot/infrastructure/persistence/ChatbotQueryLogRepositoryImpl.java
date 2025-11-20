package com.algangi.mongle.chatbot.infrastructure.persistence;

import com.algangi.mongle.chatbot.domain.model.ChatbotQueryLog;
import com.algangi.mongle.chatbot.domain.repository.ChatbotQueryLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 챗봇 질문-답변 로그 Repository 구현체
 */
@Repository
@RequiredArgsConstructor
public class ChatbotQueryLogRepositoryImpl implements ChatbotQueryLogRepository {

    private final ChatbotQueryLogJpaRepository jpaRepository;

    @Override
    public ChatbotQueryLog save(ChatbotQueryLog log) {
        return jpaRepository.save(log);
    }

    @Override
    public Page<ChatbotQueryLog> findAll(Pageable pageable) {
        return jpaRepository.findAll(pageable);
    }

    @Override
    public Page<ChatbotQueryLog> findByCreatedDateBetween(LocalDateTime startDate, LocalDateTime endDate, Pageable pageable) {
        return jpaRepository.findByCreatedDateBetween(startDate, endDate, pageable);
    }

    @Override
    public Page<ChatbotQueryLog> findByIsSuccess(Boolean isSuccess, Pageable pageable) {
        return jpaRepository.findByIsSuccess(isSuccess, pageable);
    }

    @Override
    public long count() {
        return jpaRepository.count();
    }

    @Override
    public long countByIsSuccess(Boolean isSuccess) {
        return jpaRepository.countByIsSuccess(isSuccess);
    }

    @Override
    public long countByCreatedDateBetween(LocalDateTime startDate, LocalDateTime endDate) {
        return jpaRepository.countByCreatedDateBetween(startDate, endDate);
    }

    @Override
    public List<ChatbotQueryLog> findRecentLogs(int limit) {
        return jpaRepository.findRecentLogs(limit);
    }
}
