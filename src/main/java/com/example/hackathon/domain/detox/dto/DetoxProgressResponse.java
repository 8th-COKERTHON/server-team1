package com.example.hackathon.domain.detox.dto;

import com.example.hackathon.domain.mission.entity.MissionStatus;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

public record DetoxProgressResponse(
        Long missionLogId,
        MissionStatus status,
        boolean inProgress,
        LocalDateTime startDateTime,
        LocalDateTime endDateTime,
        LocalTime endTime,
        long remainingSeconds,
        String titleMessage,
        String unlockMessage,
        int overlappingMemberCount,
        List<OverlappingMemberResponse> overlappingMembers
) {
}
