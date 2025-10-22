package com.algangi.mongle.auth.exception;

import com.algangi.mongle.global.exception.ApplicationException;

public class RateLimitExceededException extends ApplicationException {

    public RateLimitExceededException() {
        super(AuthErrorCode.VERIFICATION_CODE_TRY_EXCEEDED);
    }
}
