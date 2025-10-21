package com.algangi.mongle.auth.exception;

import com.algangi.mongle.global.exception.ApplicationException;

public class DisposableEmailException extends ApplicationException {

    public DisposableEmailException() {
        super(AuthErrorCode.DISPOSABLE_EMAIL_NOT_ALLOWED);
    }
}
