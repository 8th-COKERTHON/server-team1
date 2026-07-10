package com.example.hackathon.domain.team.dto;

import com.example.hackathon.domain.team.entity.Team;
import com.example.hackathon.domain.team.entity.TeamMember;

import java.util.List;

public record TeamDetailResponse(
        Long teamId,
        String teamName,
        String inviteCode,
        int totalBricks,
        List<Member> members
) {
    public record Member(Long userId, String nickname) {
        public static Member from(TeamMember tm) {
            return new Member(tm.getUser().getId(), tm.getUser().getNickname());
        }
    }

    public static TeamDetailResponse of(Team team, List<TeamMember> members) {
        return new TeamDetailResponse(
                team.getId(),
                team.getName(),
                team.getInviteCode(),
                team.getTotalBricks(),
                members.stream().map(Member::from).toList()
        );
    }
}
