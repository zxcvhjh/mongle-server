package com.algangi.mongle.chatbot.application.service;

import org.springframework.stereotype.Service;

import com.algangi.mongle.chatbot.domain.model.ChatbotQueryLog;
import com.algangi.mongle.chatbot.domain.repository.ChatbotQueryLogRepository;
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
    private final ChatbotQueryLogRepository queryLogRepository;

    /**
     * 사용자 질문에 대한 AI 답변을 받아옵니다.
     *
     * @param question 사용자 질문
     * @return AI 답변
     */
    public ChatbotAnswerResponse getAnswer(String question) {
        log.info("챗봇 질문 처리 시작: question={}", question);
        long startTime = System.currentTimeMillis();

        try {
            // AI 서버 호출
            AiAnswerResponse aiResponse = aiChatbotClient.getAnswer(question);
            long responseTime = System.currentTimeMillis() - startTime;

            // Presentation DTO로 변환
            ChatbotAnswerResponse response = ChatbotAnswerResponse.builder()
                .answer(aiResponse.getAnswer())
                .references(aiResponse.getReferences())
                .disclaimer(aiResponse.getDisclaimer())
                .images(aiResponse.getImages())
                .hasAnswer(aiResponse.hasAnswer())
                .hasImages(aiResponse.hasImages())
                .build();

            log.info("챗봇 질문 처리 완료: hasAnswer={}, hasImages={}, responseTime={}ms",
                response.isHasAnswer(), response.isHasImages(), responseTime);

            // 성공 로그 저장
            saveQueryLog(question, aiResponse, responseTime, true, null);

            return response;

        } catch (Exception e) {
            long responseTime = System.currentTimeMillis() - startTime;

            // 실패 로그 저장
            saveQueryLog(question, null, responseTime, false, e.getMessage());

            // 예외를 다시 던져서 에러 핸들러가 처리하도록
            throw e;
        }
    }

    /**
     * 질문-답변 로그를 저장합니다.
     * 로그 저장 실패 시에도 메인 기능은 정상 동작하도록 예외를 잡습니다.
     *
     * @param question 사용자 질문
     * @param response AI 응답 (실패 시 null)
     * @param responseTime 응답 시간 (밀리초)
     * @param isSuccess 성공 여부
     * @param errorMessage 에러 메시지 (성공 시 null)
     */
    private void saveQueryLog(String question, AiAnswerResponse response,
                              long responseTime, boolean isSuccess, String errorMessage) {
        try {
            ChatbotQueryLog queryLog = ChatbotQueryLog.builder()
                .question(question)
                .answer(response != null ? response.getAnswer() : null)
                .hasAnswer(response != null && response.hasAnswer())
                .hasImages(response != null && response.hasImages())
                .references(response != null ? response.getReferences() : null)
                .responseTimeMs(responseTime)
                .isSuccess(isSuccess)
                .errorMessage(errorMessage)
                .build();

            queryLogRepository.save(queryLog);
            log.debug("챗봇 질문-답변 로그 저장 완료: questionLength={}, isSuccess={}",
                question.length(), isSuccess);

        } catch (Exception e) {
            // 로그 저장 실패해도 메인 기능에는 영향 없도록
            log.error("챗봇 질문-답변 로그 저장 실패 (무시됨): {}", e.getMessage(), e);
        }
    }
}
