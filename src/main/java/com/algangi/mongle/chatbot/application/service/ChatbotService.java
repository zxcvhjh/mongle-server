package com.algangi.mongle.chatbot.application.service;

import org.springframework.stereotype.Service;

import com.algangi.mongle.chatbot.infrastructure.client.AiChatbotClient;
import com.algangi.mongle.chatbot.infrastructure.client.dto.AiAnswerResponse;
import com.algangi.mongle.chatbot.presentation.dto.ChatbotAnswerResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 챗봇 비즈니스 로직을 처리하는 서비스
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ChatbotService {

    private final AiChatbotClient aiChatbotClient;

    /**
     * 사용자 질문에 대한 AI 답변을 받아옵니다.
     *
     * @param question 사용자 질문
     * @return AI 답변
     */
    public ChatbotAnswerResponse getAnswer(String question) {
        log.info("챗봇 질문 처리 시작: question={}", question);

        // AI 서버 호출
        AiAnswerResponse aiResponse = aiChatbotClient.getAnswer(question);

        // Presentation DTO로 변환
        ChatbotAnswerResponse response = ChatbotAnswerResponse.builder()
            .answer(aiResponse.getAnswer())
            .references(aiResponse.getReferences())
            .disclaimer(aiResponse.getDisclaimer())
            .images(aiResponse.getImages())
            .hasAnswer(aiResponse.hasAnswer())
            .hasImages(aiResponse.hasImages())
            .build();

        log.info("챗봇 질문 처리 완료: hasAnswer={}, hasImages={}",
            response.isHasAnswer(), response.isHasImages());

        return response;
    }
}
