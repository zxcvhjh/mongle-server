package com.algangi.mongle.report.exception;

import com.algangi.mongle.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ReportErrorCode implements ErrorCode {
    TARGET_NOT_FOUND(HttpStatus.NOT_FOUND, "REPORT-001", "신고 대상을 찾을 수 없습니다."),
    DUPLICATE_REPORT(HttpStatus.CONFLICT, "REPORT-002", "이미 신고한 대상입니다."),
    SELF_REPORT_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "REPORT-003", "자기 자신을 신고할 수 없습니다."),
    REPORT_NOT_FOUND(HttpStatus.NOT_FOUND, "REPORT-004", "신고 내역을 찾을 수 없습니다."),
    REPORT_RATE_LIMIT_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS, "REPORT-005",
        "1시간당 최대 10건까지만 신고 가능합니다."),
    CANNOT_REPORT_ADMIN_OR_BOOTH(HttpStatus.BAD_REQUEST, "REPORT-006",
        "이 콘텐츠는 신고할 수 없습니다.");


    private final HttpStatus status;
    private final String code;
    private final String message;
}
