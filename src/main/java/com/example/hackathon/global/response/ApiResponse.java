package com.example.hackathon.global.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * 성공 응답 공통 포맷.
 *
 * <pre>
 * { "success": true, "status": 200, "code": "SUCCESS",
 *   "message": "...", "data": {...}, "timestamp": "..." }
 * </pre>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
        boolean success,
        int status,
        String code,
        String message,
        T data,
        Instant timestamp
) {
    private static final String SUCCESS_CODE = "SUCCESS";

    private static <T> ApiResponse<T> of(HttpStatus status, String message, T data) {
        return new ApiResponse<>(
                true,
                status.value(),
                SUCCESS_CODE,
                message,
                data,
                Instant.now().truncatedTo(ChronoUnit.MILLIS)
        );
    }

    public static <T> ApiResponse<T> ok(String message, T data) {
        return of(HttpStatus.OK, message, data);
    }

    public static <T> ApiResponse<T> ok(T data) {
        return of(HttpStatus.OK, "Success", data);
    }

    public static <T> ApiResponse<T> created(String message, T data) {
        return of(HttpStatus.CREATED, message, data);
    }
}
