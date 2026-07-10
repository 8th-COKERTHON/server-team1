package com.example.hackathon.domain.mission.controller;

import com.example.hackathon.domain.mission.dto.response.MissionConfirmResponse;
import com.example.hackathon.domain.mission.dto.response.MissionPopupResponse;
import com.example.hackathon.domain.mission.dto.response.MissionTodayResponse;
import com.example.hackathon.domain.mission.dto.response.MissionTodayStatusResponse;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Tag(name = "Mission", description = "미션 관련 API")
@RestController
@RequestMapping("/api/missions")
@RequiredArgsConstructor
public class MissionController {

    private final MissionService missionService;

    @Operation(summary = "오늘의 미션 조회 및 생성", description = "오늘 날짜에 배정된 미션을 조회합니다. 아직 없다면 신규 배정합니다.")
    @GetMapping("/today")
    public ApiResponse<MissionTodayResponse> getTodayMission(
            @RequestHeader("X-Device-Id") String deviceId
    ) {
        ZoneId seoulZone = ZoneId.of("Asia/Seoul");
        LocalDate todayDate = LocalDate.now(seoulZone);
        LocalDateTime now = LocalDateTime.now(seoulZone);

        MissionTodayResponse response = missionService.getOrCreateTodayMission(deviceId, todayDate, now);
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
}
