package com.example.hackathon.domain.user.controller;

import com.example.hackathon.domain.user.dto.DetoxTimeRequest;
import com.example.hackathon.domain.user.dto.request.UserCreateRequest;
import com.example.hackathon.domain.user.dto.request.ActiveTeamRequest;
import com.example.hackathon.domain.user.dto.response.UserCreateResponse;
import com.example.hackathon.domain.user.service.UserService;

import com.example.hackathon.global.response.ApiResponse;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "User", description = "사용자 API")
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    // 1. 사용자 생성 또는 로그인 (온보딩)
    @PostMapping
    public ResponseEntity<ApiResponse<UserCreateResponse>> createUser(@Valid @RequestBody UserCreateRequest request) {
        UserCreateResponse response = userService.getOrCreateUser(request.deviceId(), request.nickname());
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    // 2. 디톡스 시간 설정 (POST -> created 사용)
    @PostMapping("/detox-time")
    public ResponseEntity<ApiResponse<String>> setDetoxTime(@RequestBody DetoxTimeRequest request) {
        userService.setDetoxTime(request.userId(), request.startTime(), request.endTime());
        return ResponseEntity.ok(ApiResponse.created("디톡스 시간이 저장되었습니다.", "Success"));
    }

    // 3. 디톡스 시간 조회 (GET -> ok 사용)
    @GetMapping("/detox-time")
    public ResponseEntity<ApiResponse<Map<String, String>>> getDetoxTime(@RequestParam Long userId) {
        Map<String, String> data = userService.getDetoxTime(userId);
        return ResponseEntity.ok(ApiResponse.ok("디톡스 시간 조회 성공", data));
    }

    // 4. 디톡스 시간 수정 (PATCH -> ok 사용)
    @PatchMapping("/detox-time")
    public ResponseEntity<ApiResponse<String>> updateDetoxTime(@RequestBody DetoxTimeRequest request) {
        userService.updateDetoxTime(request.userId(), request.startTime(), request.endTime());
        return ResponseEntity.ok(ApiResponse.ok("디톡스 시간이 수정되었습니다.", "Success"));
    }

    // 5. 활성 팀(선택된 팀) 변경 (PATCH -> ok 사용)
    @PatchMapping("/{userId}/active-team")
    public ResponseEntity<ApiResponse<String>> selectActiveTeam(
            @PathVariable Long userId,
            @Valid @RequestBody ActiveTeamRequest request
    ) {
        userService.selectActiveTeam(userId, request.teamId());
        return ResponseEntity.ok(ApiResponse.ok("활성 팀이 성공적으로 변경되었습니다.", "Success"));
    }
}