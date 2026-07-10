package com.example.hackathon.global.response;

import com.example.hackathon.global.exception.ErrorCode;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;

/**
 * 실패 응답 공통 포맷.
 *
 * <pre>
 * { "success": false, "status": 400, "code": "...", "message": "...",
 *   "path": "/api/...", "timestamp": "...", "reasons": {} }
 * </pre>
 *
 * reasons 는 필드 단위 사유를 담는다. @Valid 검증 실패 시 { "필드명": "메시지" } 형태.
 */
public record ErrorResponse(
        boolean success,
        int status,
        String code,
        String message,
        String path,
        Instant timestamp,
        Map<String, String> reasons
) {
    public static ErrorResponse of(ErrorCode errorCode, String message, String path, Map<String, String> reasons) {
        return new ErrorResponse(
                false,
                errorCode.getStatus().value(),
                errorCode.name(),
                message,
                path,
                Instant.now().truncatedTo(ChronoUnit.MILLIS),
                reasons == null ? Map.of() : reasons
        );
    }

    public static ErrorResponse of(ErrorCode errorCode, String path) {
        return of(errorCode, errorCode.getMessage(), path, Map.of());
    }
}
