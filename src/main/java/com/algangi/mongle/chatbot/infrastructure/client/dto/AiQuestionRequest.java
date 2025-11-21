package com.algangi.mongle.chatbot.infrastructure.client.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * AI 서버로 전송하는 질문 요청 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class AiQuestionRequest {

    private String question;
}
