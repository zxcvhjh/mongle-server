package com.algangi.mongle.chatbot.infrastructure.client.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * AI 서버로부터 받는 응답 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class AiAnswerResponse {

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
     * 관련 이미지 URL 목록 (["No content"]인 경우 이미지 없음)
     */
    private List<String> images;

    /**
     * 질문이 답변 가능한지 여부
     */
    private Boolean answerable;

    public boolean hasAnswer() {
        return answer != null && !answer.trim().isEmpty();
    }

    public boolean hasImages() {
        return images != null && !images.isEmpty() && !"No content".equals(images.get(0));
    }
}
