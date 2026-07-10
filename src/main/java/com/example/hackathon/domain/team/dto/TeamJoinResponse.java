package com.example.hackathon.domain.team.dto;

import com.example.hackathon.domain.team.entity.Team;

public record TeamJoinResponse(
        Long teamId,
        String teamName,
        int memberCount
) {
    public static TeamJoinResponse of(Team team, int memberCount) {
        return new TeamJoinResponse(team.getId(), team.getName(), memberCount);
    }
}
