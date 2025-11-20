package com.algangi.mongle.chatbot.domain.repository;

import com.algangi.mongle.chatbot.domain.model.ChatbotQueryLog;

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
}
