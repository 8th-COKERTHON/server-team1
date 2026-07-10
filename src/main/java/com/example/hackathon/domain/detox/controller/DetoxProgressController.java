package com.example.hackathon.domain.detox.controller;

import com.example.hackathon.domain.detox.dto.DetoxProgressResponse;
import com.example.hackathon.domain.detox.service.DetoxProgressService;
import com.example.hackathon.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Detox", description = "디지털 디톡스 진행 관련 API")
@RestController
@RequestMapping("/api/detox")
@RequiredArgsConstructor
public class DetoxProgressController {

    private final DetoxProgressService detoxProgressService;

    @Operation(summary = "디톡스 진행 상태 조회", description = "디톡스 진행 상태와 시간이 겹치는 팀원을 조회합니다.")
    @GetMapping("/progress")
    public ApiResponse<DetoxProgressResponse> getProgress(
            @RequestHeader("X-Device-Id") String deviceId
    ) {
        return ApiResponse.ok("디톡스 진행 상태 조회 성공", detoxProgressService.getProgress(deviceId));
    }
}
