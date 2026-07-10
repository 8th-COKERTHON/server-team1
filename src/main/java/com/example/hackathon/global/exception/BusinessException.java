package com.example.hackathon.global.exception;

import lombok.Getter;

/**
 * 비즈니스 규칙 위반. 서비스 계층에서 던지면 GlobalExceptionHandler 가 공통 포맷으로 변환한다.
 */
@Getter
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    /**
     * 같은 code 를 쓰지만 상황에 따라 문구가 다를 때 사용한다.
     * 예: 팀 생성은 "이미 팀에 가입되어 있습니다.", 팀 참여는 "이미 다른 팀에 가입되어 있습니다."
     */
    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public BusinessException(ErrorCode errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.errorCode = errorCode;
    }
}
