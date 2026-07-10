package com.example.hackathon.domain.team.controller;

import com.example.hackathon.domain.team.dto.*;
import com.example.hackathon.domain.team.service.TeamService;
import com.example.hackathon.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Team", description = "팀 생성 / 초대코드 참여 / 조회")
@RestController
@RequiredArgsConstructor
public class TeamController {

    private final TeamService teamService;

    @Operation(summary = "팀 생성", description = "팀을 만들고 초대코드를 발급한다. 생성자는 자동으로 팀에 소속된다.")
    @PostMapping("/api/teams")
    public ResponseEntity<ApiResponse<TeamCreateResponse>> create(@Valid @RequestBody TeamCreateRequest request) {
        TeamCreateResponse response = teamService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("팀이 생성되었습니다.", response));
    }

    @Operation(summary = "팀 참여", description = "초대코드로 팀에 참여한다. 팀 정원은 4명이다.")
    @PostMapping("/api/teams/join")
    public ResponseEntity<ApiResponse<TeamJoinResponse>> join(@Valid @RequestBody TeamJoinRequest request) {
        TeamJoinResponse response = teamService.join(request);
        return ResponseEntity.ok(ApiResponse.ok("팀에 참여했습니다.", response));
    }

    @Operation(summary = "내 팀 목록", description = "사용자가 참여 중인 모든 팀을 반환한다. (팀 전환 바텀시트용)")
    @GetMapping("/api/users/{userId}/teams")
    public ResponseEntity<ApiResponse<MyTeamResponse>> getMyTeams(@PathVariable Long userId) {
        MyTeamResponse response = teamService.getMyTeams(userId);
        return ResponseEntity.ok(ApiResponse.ok("내 팀 목록 조회 성공", response));
    }

    @Operation(summary = "팀 상세", description = "팀 정보와 팀원 목록, 벽돌/단계를 반환한다.")
    @GetMapping("/api/teams/{teamId}")
    public ResponseEntity<ApiResponse<TeamDetailResponse>> getTeamDetail(@PathVariable Long teamId) {
        TeamDetailResponse response = teamService.getTeamDetail(teamId);
        return ResponseEntity.ok(ApiResponse.ok("팀 상세 조회 성공", response));
    }
}
