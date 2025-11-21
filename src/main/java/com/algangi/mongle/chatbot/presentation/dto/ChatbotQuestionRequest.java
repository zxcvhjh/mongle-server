package com.algangi.mongle.chatbot.presentation.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 클라이언트로부터 받는 질문 요청 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ChatbotQuestionRequest {

    @NotBlank(message = "질문은 필수입니다")
    private String question;
}
