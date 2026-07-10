package com.example.hackathon.global.exception;

import com.example.hackathon.global.response.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 서비스 계층이 명시적으로 던진 비즈니스 예외. */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusiness(BusinessException e, HttpServletRequest request) {
        ErrorCode code = e.getErrorCode();
        return ResponseEntity
                .status(code.getStatus())
                .body(ErrorResponse.of(code, e.getMessage(), request.getRequestURI(), Map.of()));
    }

    /** @Valid 검증 실패. 어떤 필드가 왜 틀렸는지 reasons 에 담는다. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException e,
                                                          HttpServletRequest request) {
        Map<String, String> reasons = new LinkedHashMap<>();
        e.getBindingResult().getFieldErrors()
                .forEach(err -> reasons.putIfAbsent(err.getField(), err.getDefaultMessage()));

        ErrorCode code = ErrorCode.COMMON_ERROR_400_INVALID_INPUT;
        return ResponseEntity
                .status(code.getStatus())
                .body(ErrorResponse.of(code, code.getMessage(), request.getRequestURI(), reasons));
    }

    /**
     * 존재하지 않는 경로.
     * 아래 Exception 핸들러가 먼저 잡아 500 으로 만들지 않도록 명시한다.
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(NoResourceFoundException e, HttpServletRequest request) {
        ErrorCode code = ErrorCode.COMMON_ERROR_404_NOT_FOUND;
        return ResponseEntity.status(code.getStatus())
                .body(ErrorResponse.of(code, request.getRequestURI()));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotAllowed(HttpRequestMethodNotSupportedException e,
                                                                HttpServletRequest request) {
        ErrorCode code = ErrorCode.COMMON_ERROR_405_METHOD_NOT_ALLOWED;
        return ResponseEntity.status(code.getStatus())
                .body(ErrorResponse.of(code, code.getMessage() + ": " + e.getMethod(),
                        request.getRequestURI(), Map.of()));
    }

    /** 필수 요청 헤더가 없을 때. reasons 에 빠진 헤더명을 담아 400 으로 응답한다. */
    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ErrorResponse> handleMissingHeader(MissingRequestHeaderException e,
                                                             HttpServletRequest request) {
        ErrorCode code = ErrorCode.COMMON_ERROR_400_INVALID_INPUT;
        return ResponseEntity.status(code.getStatus())
                .body(ErrorResponse.of(code, "필수 요청 헤더가 없습니다.", request.getRequestURI(),
                        Map.of(e.getHeaderName(), "필수 헤더입니다.")));
    }

    /** 요청 본문을 파싱하지 못할 때(형식 오류, 빈 본문 등). 400 으로 응답한다. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadableBody(HttpMessageNotReadableException e,
                                                              HttpServletRequest request) {
        ErrorCode code = ErrorCode.COMMON_ERROR_400_INVALID_INPUT;
        return ResponseEntity.status(code.getStatus())
                .body(ErrorResponse.of(code, "요청 본문을 읽을 수 없습니다.", request.getRequestURI(), Map.of()));
    }

    /** 경로/쿼리 파라미터의 타입이 맞지 않을 때. 400 으로 응답한다. */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException e,
                                                            HttpServletRequest request) {
        ErrorCode code = ErrorCode.COMMON_ERROR_400_INVALID_INPUT;
        return ResponseEntity.status(code.getStatus())
                .body(ErrorResponse.of(code, code.getMessage(), request.getRequestURI(),
                        Map.of(e.getName(), "타입이 올바르지 않습니다.")));
    }

    /**
     * 예상 못한 에러.
     * 스택트레이스는 로그에만 남기고 응답에는 내부 정보를 노출하지 않는다.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception e, HttpServletRequest request) {
        log.error("처리되지 않은 예외: {} {}", request.getMethod(), request.getRequestURI(), e);

        ErrorCode code = ErrorCode.COMMON_ERROR_500_INTERNAL;
        return ResponseEntity.status(code.getStatus())
                .body(ErrorResponse.of(code, request.getRequestURI()));
    }
}
