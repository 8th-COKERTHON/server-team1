package com.example.hackathon.domain.mission.dto.response;

import com.example.hackathon.domain.mission.entity.MissionStatus;
import com.example.hackathon.domain.mission.entity.UserMissionLog;

import java.time.LocalDateTime;

public record MissionConfirmResponse(
        Long missionLogId,
        MissionStatus status,
        LocalDateTime deadlineAt,
        long remainingSeconds
) {
    public static MissionConfirmResponse of(UserMissionLog log, long remainingSeconds) {
        return new MissionConfirmResponse(
                log.getId(),
                log.getStatus(),
                log.getDeadlineAt(),
                remainingSeconds
        );
    }
}
