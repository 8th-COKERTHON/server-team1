package com.example.hackathon.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * 응답 code 는 enum 이름을 그대로 쓴다. 규칙: {도메인}_ERROR_{상태코드}_{사유}
 * 새 에러가 필요하면 여기에 상수를 추가하고, 서비스에서 BusinessException 으로 던진다.
 */
@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // 공통
    COMMON_ERROR_400_INVALID_INPUT(HttpStatus.BAD_REQUEST, "요청값이 올바르지 않습니다."),
    COMMON_ERROR_404_NOT_FOUND(HttpStatus.NOT_FOUND, "요청한 경로를 찾을 수 없습니다."),
    COMMON_ERROR_405_METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "지원하지 않는 HTTP 메서드입니다."),
    COMMON_ERROR_500_INTERNAL(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다."),

    // 사용자
    USER_ERROR_404_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 사용자입니다."),

    // 팀
    TEAM_ERROR_404_INVALID_INVITE_CODE(HttpStatus.NOT_FOUND, "유효하지 않은 초대코드입니다."),
    TEAM_ERROR_409_TEAM_FULL(HttpStatus.CONFLICT, "팀 정원이 가득 찼습니다."),
    TEAM_ERROR_409_ALREADY_IN_TEAM(HttpStatus.CONFLICT, "이미 다른 팀에 가입되어 있습니다."),

    // 이미지
    IMAGE_ERROR_400_INVALID_CONTENT_TYPE(HttpStatus.BAD_REQUEST, "지원하지 않는 이미지 형식입니다."),
    IMAGE_ERROR_500_PRESIGNED_URL_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "이미지 업로드 URL 발급에 실패했습니다."),

    // 미션
    MISSION_ERROR_400_BEFORE_DETOX_START(HttpStatus.BAD_REQUEST, "디톡스 시작 시간 이전입니다."),
    MISSION_ERROR_400_DETOX_TIME_NOT_SET(HttpStatus.BAD_REQUEST, "디톡스 시간이 설정되지 않았습니다."),
    MISSION_ERROR_500_NO_MISSION_DATA(HttpStatus.INTERNAL_SERVER_ERROR, "미션 데이터가 존재하지 않습니다.");

    private final HttpStatus status;
    private final String message;
}
