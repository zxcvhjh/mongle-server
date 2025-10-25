package com.algangi.mongle.post.exception;

import com.algangi.mongle.global.exception.ApplicationException;
import com.algangi.mongle.global.exception.ErrorCode;

public class RateLimitException extends ApplicationException {

    private final String customMessage;

    public RateLimitException(ErrorCode errorCode, long minutes, long seconds) {
        super(errorCode);
        this.customMessage = String.format("다음 글 작성까지 남은 시간: %d:%02d", minutes, seconds);
    }

    @Override
    public String getMessage() {
        return customMessage;
    }
}
