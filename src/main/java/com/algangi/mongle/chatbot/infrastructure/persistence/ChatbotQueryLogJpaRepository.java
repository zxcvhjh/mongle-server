package com.algangi.mongle.chatbot.infrastructure.persistence;

import com.algangi.mongle.chatbot.domain.model.ChatbotQueryLog;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 챗봇 질문-답변 로그 JPA Repository
 */
public interface ChatbotQueryLogJpaRepository extends JpaRepository<ChatbotQueryLog, String> {
}
