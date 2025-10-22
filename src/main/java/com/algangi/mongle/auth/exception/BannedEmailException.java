package com.algangi.mongle.auth.exception;

import com.algangi.mongle.global.exception.ApplicationException;

public class BannedEmailException extends ApplicationException {

    public BannedEmailException() {
        super(AuthErrorCode.EMAIL_IS_BANNED);
    }
}