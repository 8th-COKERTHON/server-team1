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
    TEAM_ERROR_404_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 팀입니다."),
    TEAM_ERROR_404_INVALID_INVITE_CODE(HttpStatus.NOT_FOUND, "유효하지 않은 초대코드입니다."),
    TEAM_ERROR_409_TEAM_FULL(HttpStatus.CONFLICT, "팀 정원이 가득 찼습니다."),
    TEAM_ERROR_409_ALREADY_IN_TEAM(HttpStatus.CONFLICT, "이미 참여한 팀입니다."),

    // 이미지
    IMAGE_ERROR_400_INVALID_CONTENT_TYPE(HttpStatus.BAD_REQUEST, "지원하지 않는 이미지 형식입니다."),
    IMAGE_ERROR_400_EMPTY_FILE(HttpStatus.BAD_REQUEST, "이미지 파일이 비어 있습니다."),
    IMAGE_ERROR_400_SIZE_EXCEEDED(HttpStatus.BAD_REQUEST, "이미지 파일은 10MB를 초과할 수 없습니다."),
    IMAGE_ERROR_500_UPLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "이미지 업로드에 실패했습니다."),

    // 미션
    MISSION_ERROR_400_BEFORE_DETOX_START(HttpStatus.BAD_REQUEST, "디톡스 시작 시간 이전입니다."),
    MISSION_ERROR_400_DETOX_TIME_NOT_SET(HttpStatus.BAD_REQUEST, "디톡스 시간이 설정되지 않았습니다."),
    MISSION_ERROR_500_NO_MISSION_DATA(HttpStatus.INTERNAL_SERVER_ERROR, "미션 데이터가 존재하지 않습니다."),
    MISSION_ERROR_404_ACTIVE_NOT_FOUND(HttpStatus.NOT_FOUND, "활성화된 미션이 존재하지 않습니다."),
    MISSION_ERROR_409_CONFLICT(HttpStatus.CONFLICT, "오늘 미션 생성 중 중복 데이터가 발생했습니다."),
    MISSION_ERROR_404_NOT_FOUND(HttpStatus.NOT_FOUND, "미션 데이터가 존재하지 않습니다."),
    MISSION_ERROR_400_DEADLINE_EXCEEDED(HttpStatus.BAD_REQUEST, "미션 제한 시간을 초과했습니다."),
    MISSION_ERROR_400_ALREADY_COMPLETED(HttpStatus.BAD_REQUEST, "이미 성공한 미션입니다."),
    MISSION_ERROR_400_ALREADY_FAILED(HttpStatus.BAD_REQUEST, "이미 실패한 미션입니다."),
    MISSION_ERROR_400_ALREADY_CERTIFIED(HttpStatus.BAD_REQUEST, "이미 인증된 미션입니다. 사진 재등록 API를 이용해주세요."),
    MISSION_ERROR_400_CERTIFICATION_NOT_FOUND(HttpStatus.BAD_REQUEST, "재등록할 인증 사진이 없습니다."),
    MISSION_ERROR_400_DETOX_ALREADY_ENDED(HttpStatus.BAD_REQUEST, "디톡스 종료 후에는 인증 사진을 변경할 수 없습니다."),
    MISSION_ERROR_400_NOT_CERTIFIED(HttpStatus.BAD_REQUEST, "미션 인증 완료 후 디톡스 진행 화면을 조회할 수 있습니다."),
    MISSION_ERROR_400_INVALID_TRANSITION(HttpStatus.BAD_REQUEST, "잘못된 상태 전이입니다.");

    private final HttpStatus status;
    private final String message;
}
