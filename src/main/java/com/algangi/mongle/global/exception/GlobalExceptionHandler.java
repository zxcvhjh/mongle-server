package com.algangi.mongle.global.exception;

import com.algangi.mongle.auth.exception.BannedEmailException;
import com.algangi.mongle.global.dto.ApiResponse;
import com.algangi.mongle.post.exception.RateLimitException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.List;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(ApplicationException.class)
    public ResponseEntity<ApiResponse<ErrorInfo>> handleApplicationException(
        ApplicationException exception) {

        ErrorCode errorCode = exception.getErrorCode();

        // 로깅: 에러 코드와 메시지, 추가 정보 기록
        if (exception.getCause() != null) {
            log.error("Application Exception: status={}, code={}, message={}, errorInfo={}",
                errorCode.getStatus(), errorCode.getCode(), errorCode.getMessage(),
                exception.getErrorInfo(), exception.getCause());
        } else {
            log.warn("Application Exception: status={}, code={}, message={}, errorInfo={}",
                errorCode.getStatus(), errorCode.getCode(), errorCode.getMessage(),
                exception.getErrorInfo());
        }

        // BannedEmailException은 errorInfo 없이 반환
        if (exception instanceof BannedEmailException) {
            return ResponseEntity.status(errorCode.getStatus())
                .body(ApiResponse.error(errorCode.getCode(), errorCode.getMessage()));
        }

        // 일반 ApplicationException은 errorInfo 포함하여 반환
        return ResponseEntity.status(errorCode.getStatus())
            .body(ApiResponse.error(errorCode.getCode(), errorCode.getMessage(),
                ErrorInfo.of(exception.getErrorInfo())));
    }

    @ExceptionHandler(RateLimitException.class)
    public ResponseEntity<ApiResponse<Void>> handleRateLimitException(
        RateLimitException exception) {
        ErrorCode errorCode = exception.getErrorCode();
        log.warn("Rate Limit Exception: code={}, message={}", errorCode.getCode(), errorCode.getMessage());
        return ResponseEntity.status(errorCode.getStatus())
            .body(ApiResponse.error(errorCode.getCode(), exception.getMessage()));
    }


    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<ErrorInfo>> handleIllegalArgumentException(
        IllegalArgumentException exception) {
        log.warn("IllegalArgumentException: {}", exception.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ApiResponse.error(HttpStatus.BAD_REQUEST.getReasonPhrase(),
                exception.getMessage()));
    }

    @ExceptionHandler(PessimisticLockingFailureException.class)
    public ResponseEntity<ApiResponse<ErrorInfo>> handlePessimisticLockingFailure(
        PessimisticLockingFailureException exception) {
        log.warn("PessimisticLockingFailureException: {}", exception.getMessage());
        return ResponseEntity.status(HttpStatus.LOCKED)
            .body(ApiResponse.error(HttpStatus.LOCKED.getReasonPhrase(),
                exception.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<List<ErrorInfo>>> handleValidationException(
        MethodArgumentNotValidException exception) {
        List<ErrorInfo> errors = exception.getBindingResult().getFieldErrors().stream()
            .map(fieldError -> ErrorInfo.of(Map.of(
                fieldError.getField(),
                fieldError.getDefaultMessage() != null ? fieldError.getDefaultMessage()
                    : "유효하지 않은 값입니다."
            )))
            .toList();

        log.warn("Validation Exception: {}", errors);
        return ResponseEntity.badRequest()
            .body(ApiResponse.error("VALIDATION_FAILED", "요청 유효성 검사에 실패했습니다.", errors));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleTypeMismatchException(
        MethodArgumentTypeMismatchException exception) {
        String message = String.format("'%s' 파라미터에 유효하지 않은 값이 입력되었습니다.", exception.getName());
        log.warn("Type Mismatch Exception: parameter={}, requiredType={}",
            exception.getName(), exception.getRequiredType());
        return ResponseEntity.badRequest()
            .body(ApiResponse.error("INVALID_PARAMETER_TYPE", message));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleException(
        HttpRequestMethodNotSupportedException exception) {
        log.warn("HTTP Method Not Supported: {}", exception.getMethod());
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
            .body(ApiResponse.error("HTTP_METHOD_NOT_SUPPORTED", "지원하지 않는 HTTP 메소드입니다."));
    }
}
