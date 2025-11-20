package com.algangi.mongle.chatbot.presentation.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 클라이언트에게 전송하는 답변 응답 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatbotAnswerResponse {

    /**
     * AI가 생성한 답변 텍스트 (null 가능)
     */
    private String answer;

    /**
     * 참고 URL
     */
    private String references;

    /**
     * 면책 조항
     */
    private String disclaimer;

    /**
     * 관련 이미지 URL 목록
     */
    private List<String> images;

    /**
     * 답변이 있는지 확인
     */
    private boolean hasAnswer;

    /**
     * 이미지가 있는지 확인
     */
    private boolean hasImages;
}
