package com.example.hackathon.domain.mission.dto.response;

import com.example.hackathon.domain.mission.entity.MissionStatus;
import com.example.hackathon.domain.mission.entity.UserMissionLog;

import java.time.LocalDateTime;
import java.time.LocalTime;

public record MissionCertificationResponse(
        Long missionLogId,
        MissionStatus status,
        String imageUrl,
        LocalDateTime completedAt,
        LocalTime detoxEndTime,
        boolean canRetake
) {
    public static MissionCertificationResponse of(
            UserMissionLog log,
            LocalTime detoxEndTime,
            boolean canRetake
    ) {
        return new MissionCertificationResponse(
                log.getId(), log.getStatus(), log.getImageUrl(), log.getCompletedAt(),
                detoxEndTime, canRetake
        );
    }
}
