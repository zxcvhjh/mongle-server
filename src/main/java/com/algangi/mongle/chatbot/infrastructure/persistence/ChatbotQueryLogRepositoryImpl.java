package com.algangi.mongle.chatbot.infrastructure.persistence;

import com.algangi.mongle.chatbot.domain.model.ChatbotQueryLog;
import com.algangi.mongle.chatbot.domain.repository.ChatbotQueryLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

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
}
