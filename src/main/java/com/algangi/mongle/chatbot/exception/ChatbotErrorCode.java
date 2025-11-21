package com.algangi.mongle.chatbot.exception;

import org.springframework.http.HttpStatus;

import com.algangi.mongle.global.exception.ErrorCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ChatbotErrorCode implements ErrorCode {

    AI_SERVER_CONNECTION_FAILED(HttpStatus.SERVICE_UNAVAILABLE, "CHATBOT-001",
        "AI 챗봇 서버에 연결할 수 없습니다. 잠시 후 다시 시도해주세요."),
    AI_SERVER_TIMEOUT(HttpStatus.GATEWAY_TIMEOUT, "CHATBOT-002",
        "AI 챗봇 서버 응답 시간이 초과되었습니다. 잠시 후 다시 시도해주세요."),
    AI_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "CHATBOT-003",
        "AI 챗봇 서버에서 오류가 발생했습니다."),
    INVALID_QUESTION(HttpStatus.BAD_REQUEST, "CHATBOT-004",
        "질문이 비어있거나 형식이 올바르지 않습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;

    @Override
    public HttpStatus getStatus() {
        return status;
    }

    @Override
    public String getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
