package com.kanta.github.common;

import com.kanta.github.infrastructure.ratelimit.RateLimitExceededException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFound(NoResourceFoundException exception) {
        var body = new ApiResponse<Void>(404, "요청한 경로를 찾을 수 없습니다.", null, "NOT_FOUND");
        return ResponseEntity.status(404).body(body);
    }

    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<ApiResponse<Void>> handleRateLimitExceeded(RateLimitExceededException exception) {
        var body = new ApiResponse<Void>(
            exception.getStatus().value(),
            exception.getMessage(),
            null,
            exception.getErrorCode()
        );
        return ResponseEntity.status(exception.getStatus())
            .header("Retry-After", String.valueOf(exception.getRetryAfterSeconds()))
            .body(body);
    }

    @ExceptionHandler(DomainException.class)
    public ResponseEntity<ApiResponse<Void>> handleDomainException(DomainException exception) {
        var body = new ApiResponse<Void>(
            exception.getStatus().value(),
            exception.getMessage(),
            null,
            exception.getErrorCode()
        );
        return ResponseEntity.status(exception.getStatus()).body(body);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(MethodArgumentNotValidException exception) {
        var fieldError = exception.getBindingResult().getFieldErrors().stream().findFirst();
        var message = fieldError.map(error -> error.getDefaultMessage()).orElse("요청 값이 올바르지 않습니다.");
        var body = new ApiResponse<Void>(400, message, null, "VALIDATION_ERROR");
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ApiResponse<Void>> handleMalformedRequest(Exception exception) {
        var body = new ApiResponse<Void>(400, "요청 값이 올바르지 않습니다.", null, "VALIDATION_ERROR");
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpectedException(Exception exception) {
        log.error("처리되지 않은 예외가 발생했습니다.", exception);
        var body = new ApiResponse<Void>(500, "서버 내부 오류가 발생했습니다.", null, "INTERNAL_SERVER_ERROR");
        return ResponseEntity.internalServerError().body(body);
    }
}
