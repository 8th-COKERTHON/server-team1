package com.example.hackathon.domain.mission.dto.response;

import com.example.hackathon.domain.mission.entity.MissionStatus;
import com.example.hackathon.domain.mission.entity.UserMissionLog;

import java.time.LocalDateTime;

public record MissionTodayStatusResponse(
        Long missionLogId,
        MissionStatus status,
        boolean popupRequired,
        LocalDateTime popupShownAt,
        LocalDateTime assignedAt,
        LocalDateTime deadlineAt,
        long remainingSeconds,
        boolean expired
) {
    public static MissionTodayStatusResponse of(UserMissionLog log, boolean popupRequired, long remainingSeconds, boolean expired) {
        return new MissionTodayStatusResponse(
                log.getId(),
                log.getStatus(),
                popupRequired,
                log.getPopupShownAt(),
                log.getAssignedAt(),
                log.getDeadlineAt(),
                remainingSeconds,
                expired
        );
    }
}
