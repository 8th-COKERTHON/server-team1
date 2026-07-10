package com.example.hackathon.domain.team.dto;

import com.example.hackathon.domain.team.entity.Team;

public record TeamCreateResponse(
        Long teamId,
        String teamName,
        String inviteCode
) {
    public static TeamCreateResponse from(Team team) {
        return new TeamCreateResponse(team.getId(), team.getName(), team.getInviteCode());
    }
}
