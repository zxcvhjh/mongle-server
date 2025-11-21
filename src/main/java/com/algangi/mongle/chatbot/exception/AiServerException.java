package com.algangi.mongle.chatbot.exception;

import com.algangi.mongle.global.exception.ApplicationException;
import com.algangi.mongle.global.exception.ErrorCode;

public class AiServerException extends ApplicationException {

    public AiServerException(ErrorCode errorCode) {
        super(errorCode);
    }

    public AiServerException(ErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }
}
