package com.example.hackathon.domain.team.dto;

import com.example.hackathon.domain.team.entity.Team;

import java.util.List;

/**
 * 내가 속한 팀 목록 (팀 전환 바텀시트용).
 */
public record MyTeamResponse(
        List<Item> teams
) {
    public record Item(Long teamId, String teamName, int memberCount) {
        public static Item of(Team team, int memberCount) {
            return new Item(team.getId(), team.getName(), memberCount);
        }
    }

    public static MyTeamResponse of(List<Item> teams) {
        return new MyTeamResponse(teams);
    }
}
