package com.algangi.mongle.chatbot.presentation.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.algangi.mongle.chatbot.application.service.ChatbotService;
import com.algangi.mongle.chatbot.presentation.dto.ChatbotAnswerResponse;
import com.algangi.mongle.chatbot.presentation.dto.ChatbotQuestionRequest;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 챗봇 REST API 컨트롤러
 * ApiResponseAdvice에 의해 자동으로 ApiResponse로 래핑됨
 */
@RestController
@RequestMapping("/api/v1/chatbot")
@RequiredArgsConstructor
@Slf4j
public class ChatbotRestController {

    private final ChatbotService chatbotService;

    /**
     * AI 챗봇 질문 답변 API
     *
     * @param request 질문 요청
     * @return AI 답변
     */
    @PostMapping("/ask")
    public ChatbotAnswerResponse askQuestion(@Valid @RequestBody ChatbotQuestionRequest request) {
        log.info("챗봇 질문 요청: question={}", request.getQuestion());
        return chatbotService.getAnswer(request.getQuestion());
    }
}
