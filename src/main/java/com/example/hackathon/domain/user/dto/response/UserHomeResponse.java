package com.example.hackathon.domain.user.dto.response;

import java.util.List;

public record UserHomeResponse(
    String mode, // "TEAM" or "INDIVIDUAL"
    String detoxStartTime,
    String detoxEndTime,
    Long selectedTeamId,
    String selectedTeamName,
    int totalBricks,
    int stage,
    List<HomeMemberStatus> members,
    HomePopupInfo popup
) {
    public record HomeMemberStatus(
        Long userId,
        String nickname,
        boolean isSuccess,
        String imageUrl
    ) {}

    public record HomePopupInfo(
        boolean showPopup,
        List<String> failedMemberNames
    ) {}
}
