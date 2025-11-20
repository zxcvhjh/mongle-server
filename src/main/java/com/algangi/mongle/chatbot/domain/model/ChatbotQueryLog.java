package com.algangi.mongle.chatbot.domain.model;

import com.algangi.mongle.global.annotation.ULID;
import com.algangi.mongle.global.entity.TimeBaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 챗봇 질문-답변 로그 엔티티
 * 사용자의 질문과 AI 응답을 기록하여 서비스 개선에 활용
 */
@Entity
@Table(name = "chatbot_query_log")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Getter
public class ChatbotQueryLog extends TimeBaseEntity {

    @Id
    @ULID
    private String id;

    /**
     * 사용자 질문
     */
    @Column(nullable = false, length = 2000)
    private String question;

    /**
     * AI 답변 (긴 텍스트 가능)
     */
    @Lob
    @Column(columnDefinition = "TEXT")
    private String answer;

    /**
     * 답변 존재 여부
     */
    @Column(nullable = false)
    private Boolean hasAnswer;

    /**
     * 이미지 존재 여부
     */
    @Column(nullable = false)
    private Boolean hasImages;

    /**
     * 참고 URL
     */
    @Column(length = 500)
    private String references;

    /**
     * 응답 시간 (밀리초)
     */
    @Column(nullable = false)
    private Long responseTimeMs;

    /**
     * 성공 여부
     */
    @Column(nullable = false)
    private Boolean isSuccess;

    /**
     * 에러 메시지 (실패 시)
     */
    @Column(length = 1000)
    private String errorMessage;
}
