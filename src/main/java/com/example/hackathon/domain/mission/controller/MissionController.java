package com.example.hackathon.domain.mission.controller;

import com.example.hackathon.domain.mission.dto.response.MissionTodayResponse;
import com.example.hackathon.domain.mission.service.MissionService;
import com.example.hackathon.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
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
}
