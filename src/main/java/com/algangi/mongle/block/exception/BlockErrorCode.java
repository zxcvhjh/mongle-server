package com.algangi.mongle.block.exception;

import com.algangi.mongle.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum BlockErrorCode implements ErrorCode {

    CANNOT_BLOCK_ADMIN_OR_BOOTH(HttpStatus.BAD_REQUEST, "BLOCK-001", "관리자 또는 부스 계정은 차단할 수 없습니다."),
    CANNOT_BLOCK_SELF(HttpStatus.BAD_REQUEST, "BLOCK-002", "자기 자신을 차단할 수 없습니다.");


    private final HttpStatus status;
    private final String code;
    private final String message;
}
