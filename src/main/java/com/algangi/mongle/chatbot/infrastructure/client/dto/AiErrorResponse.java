package com.algangi.mongle.chatbot.infrastructure.client.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * AI 서버 에러 응답 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class AiErrorResponse {

    private String error;
}
