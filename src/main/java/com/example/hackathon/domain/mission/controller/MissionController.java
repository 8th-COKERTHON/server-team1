package com.example.hackathon.domain.mission.controller;

import com.example.hackathon.domain.mission.dto.response.MissionConfirmResponse;
import com.example.hackathon.domain.mission.dto.response.MissionPopupResponse;
import com.example.hackathon.domain.mission.dto.response.MissionTodayResponse;
import com.example.hackathon.domain.mission.dto.response.MissionTodayStatusResponse;
import com.example.hackathon.domain.mission.dto.response.MissionCertificationResponse;
import com.example.hackathon.domain.mission.dto.response.MissionCertificationRetakeResponse;
import com.example.hackathon.domain.mission.service.MissionCertificationService;
import com.example.hackathon.domain.mission.service.MissionService;
import com.example.hackathon.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "Mission", description = "미션 관련 API")
@RestController
@RequestMapping("/api/missions")
@RequiredArgsConstructor
public class MissionController {

    private final MissionService missionService;
    private final MissionCertificationService missionCertificationService;

    @Operation(summary = "오늘의 미션 조회 및 생성", description = "오늘 날짜에 배정된 미션을 조회합니다. 아직 없다면 신규 배정합니다.")
    @GetMapping("/today")
    public ApiResponse<MissionTodayResponse> getTodayMission(
            @RequestHeader("X-Device-Id") String deviceId
    ) {
        MissionTodayResponse response = missionService.getOrCreateTodayMission(deviceId);
        return ApiResponse.ok("오늘의 미션 조회 성공", response);
    }

    @Operation(summary = "오늘의 미션 상태 조회", description = "오늘 미션의 상태, 팝업 필요 여부, 남은 시간 등을 조회합니다.")
    @GetMapping("/today/status")
    public ApiResponse<MissionTodayStatusResponse> getTodayMissionStatus(
            @RequestHeader("X-Device-Id") String deviceId
    ) {
        MissionTodayStatusResponse response = missionService.getTodayMissionStatus(deviceId);
        return ApiResponse.ok("오늘의 미션 상태 조회 성공", response);
    }

    @Operation(summary = "미션 팝업 최초 노출 처리", description = "미션 팝업 노출 시 최초 노출 시각을 기록합니다.")
    @PostMapping("/today/popup")
    public ApiResponse<MissionPopupResponse> recordPopupShown(
            @RequestHeader("X-Device-Id") String deviceId
    ) {
        MissionPopupResponse response = missionService.recordPopupShown(deviceId);
        return ApiResponse.ok("미션 팝업 노출 처리 성공", response);
    }

    @Operation(summary = "미션 확인 완료 처리", description = "오늘의 미션을 확인 완료(CONFIRMED) 상태로 전환합니다.")
    @PostMapping("/today/confirm")
    public ApiResponse<MissionConfirmResponse> confirmMission(
            @RequestHeader("X-Device-Id") String deviceId
    ) {
        MissionConfirmResponse response = missionService.confirmMission(deviceId);
        return ApiResponse.ok("미션 확인 처리 성공", response);
    }

    @Operation(summary = "미션 인증 사진 최초 등록", description = "인증 사진을 업로드하고 오늘의 미션을 성공 처리합니다.")
    @PostMapping(value = "/today/certification", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<MissionCertificationResponse> certifyMission(
            @RequestHeader("X-Device-Id") String deviceId,
            @RequestPart("image") MultipartFile image
    ) {
        MissionCertificationResponse response = missionCertificationService.certify(deviceId, image);
        return ApiResponse.ok("미션 인증 성공", response);
    }

    @Operation(summary = "미션 인증 사진 재등록", description = "디톡스 종료 전 기존 인증 사진을 교체합니다.")
    @PatchMapping(value = "/today/certification", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<MissionCertificationRetakeResponse> retakeMissionCertification(
            @RequestHeader("X-Device-Id") String deviceId,
            @RequestPart("image") MultipartFile image
    ) {
        MissionCertificationRetakeResponse response = missionCertificationService.retake(deviceId, image);
        return ApiResponse.ok("미션 인증 사진 재등록 성공", response);
    }
}
