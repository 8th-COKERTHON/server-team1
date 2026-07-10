package com.example.hackathon.domain.mission.dto.response;

import com.example.hackathon.domain.mission.entity.MissionStatus;
import com.example.hackathon.domain.mission.entity.UserMissionLog;

import java.time.LocalDateTime;

public record MissionPopupResponse(
        Long missionLogId,
        MissionStatus status,
        LocalDateTime popupShownAt,
        LocalDateTime deadlineAt,
        long remainingSeconds,
        boolean popupRequired
) {
    public static MissionPopupResponse of(UserMissionLog log, long remainingSeconds, boolean popupRequired) {
        return new MissionPopupResponse(
                log.getId(),
                log.getStatus(),
                log.getPopupShownAt(),
                log.getDeadlineAt(),
                remainingSeconds,
                popupRequired
        );
    }
}
