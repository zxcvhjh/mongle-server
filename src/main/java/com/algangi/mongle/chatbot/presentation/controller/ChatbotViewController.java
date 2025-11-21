package com.algangi.mongle.chatbot.presentation.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.algangi.mongle.chatbot.application.service.ChatbotService;
import com.algangi.mongle.chatbot.presentation.dto.ChatbotAnswerResponse;
import com.algangi.mongle.chatbot.presentation.dto.ChatbotQuestionRequest;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 챗봇 웹 페이지 컨트롤러 (SSR)
 */
@Controller
@RequestMapping("/chatbot")
@RequiredArgsConstructor
@Slf4j
public class ChatbotViewController {

    private final ChatbotService chatbotService;

    /**
     * 챗봇 웹 페이지 렌더링
     *
     * @return Thymeleaf 템플릿 이름
     */
    @GetMapping
    public String chatbotPage() {
        log.info("챗봇 페이지 접속");
        return "chatbot/index";
    }

    /**
     * 이용약관 페이지 렌더링
     *
     * @return Thymeleaf 템플릿 이름
     */
    @GetMapping("/terms")
    public String termsPage() {
        log.info("챗봇 이용약관 페이지 접속");
        return "chatbot/terms";
    }

    /**
     * AJAX 요청으로 AI 답변 받기
     *
     * @param request 질문 요청
     * @return AI 답변
     */
    @PostMapping("/ask")
    @ResponseBody
    public ChatbotAnswerResponse askQuestion(@Valid @RequestBody ChatbotQuestionRequest request) {
        log.info("챗봇 질문 요청: question={}", request.getQuestion());
        return chatbotService.getAnswer(request.getQuestion());
    }
}
