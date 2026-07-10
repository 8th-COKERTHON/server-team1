package com.example.hackathon.domain.user.dto.request;

import jakarta.validation.constraints.NotNull;

public record ActiveTeamRequest(
    @NotNull(message = "팀 ID는 필수값입니다.")
    Long teamId
) {}
