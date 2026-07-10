package com.example.hackathon.domain.mission.dto.response;

import com.example.hackathon.domain.mission.entity.MissionStatus;
import com.example.hackathon.domain.mission.entity.UserMissionLog;

import java.time.LocalDateTime;
import java.time.LocalTime;

public record MissionCertificationRetakeResponse(
        Long missionLogId,
        MissionStatus status,
        String imageUrl,
        LocalDateTime completedAt,
        LocalDateTime updatedAt,
        LocalTime detoxEndTime,
        boolean canRetake
) {
    public static MissionCertificationRetakeResponse of(
            UserMissionLog log,
            LocalDateTime updatedAt,
            LocalTime detoxEndTime,
            boolean canRetake
    ) {
        return new MissionCertificationRetakeResponse(
                log.getId(), log.getStatus(), log.getImageUrl(), log.getCompletedAt(),
                updatedAt, detoxEndTime, canRetake
        );
    }
}
