package com.algangi.mongle.chatbot.presentation.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

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
}
