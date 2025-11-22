package com.algangi.mongle.chatbot.infrastructure.client;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import com.algangi.mongle.chatbot.exception.AiServerException;
import com.algangi.mongle.chatbot.exception.ChatbotErrorCode;
import com.algangi.mongle.chatbot.infrastructure.client.dto.AiAnswerResponse;
import com.algangi.mongle.chatbot.infrastructure.client.dto.AiQuestionRequest;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * AI 챗봇 서버와 통신하는 클라이언트
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AiChatbotClient {

    private static final String AI_RESPONSE_ENDPOINT = "/ai/ai-response";

    private final RestTemplate aiChatbotRestTemplate;

    /**
     * AI 서버에 질문을 전송하고 응답을 받습니다.
     *
     * @param question 사용자 질문
     * @return AI 서버 응답
     * @throws AiServerException AI 서버 통신 오류 시
     */
    public AiAnswerResponse getAnswer(String question) {
        AiQuestionRequest request = new AiQuestionRequest(question);

        try {
            log.info("AI 서버 요청 시작: question={}", question);
            long startTime = System.currentTimeMillis();

            ResponseEntity<AiAnswerResponse> response = aiChatbotRestTemplate.postForEntity(
                AI_RESPONSE_ENDPOINT,
                request,
                AiAnswerResponse.class
            );

            long elapsed = System.currentTimeMillis() - startTime;
            AiAnswerResponse body = response.getBody();
            log.info("AI 서버 응답 완료: {}ms, answerable={}", elapsed,
                body != null ? body.getAnswerable() : null);

            if (response.getStatusCode() == HttpStatus.OK && body != null) {
                return body;
            } else {
                throw new AiServerException(ChatbotErrorCode.AI_SERVER_ERROR);
            }

        } catch (HttpClientErrorException e) {
            log.error("AI 서버 클라이언트 오류 (4xx): status={}, body={}",
                e.getStatusCode(), e.getResponseBodyAsString());
            throw new AiServerException(ChatbotErrorCode.INVALID_QUESTION, e);

        } catch (HttpServerErrorException e) {
            log.error("AI 서버 내부 오류 (5xx): status={}, body={}",
                e.getStatusCode(), e.getResponseBodyAsString());
            throw new AiServerException(ChatbotErrorCode.AI_SERVER_ERROR, e);

        } catch (ResourceAccessException e) {
            log.error("AI 서버 연결 실패 또는 타임아웃: {}", e.getMessage());

            if (e.getCause() instanceof java.net.SocketTimeoutException) {
                throw new AiServerException(ChatbotErrorCode.AI_SERVER_TIMEOUT, e);
            } else {
                throw new AiServerException(ChatbotErrorCode.AI_SERVER_CONNECTION_FAILED, e);
            }
        }
    }
}
