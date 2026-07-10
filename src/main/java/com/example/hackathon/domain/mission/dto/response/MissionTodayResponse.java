package com.example.hackathon.domain.mission.dto.response;

import com.example.hackathon.domain.mission.entity.Difficulty;
import com.example.hackathon.domain.mission.entity.MissionStatus;
import com.example.hackathon.domain.mission.entity.UserMissionLog;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record MissionTodayResponse(
        Long missionLogId,
        Long missionId,
        String title,
        Difficulty difficulty,
        LocalDate targetDate,
        MissionStatus status,
        LocalDateTime assignedAt,
        LocalDateTime popupShownAt,
        LocalDateTime deadlineAt,
        Boolean popupRequired
) {
    public static MissionTodayResponse from(UserMissionLog log) {
        return new MissionTodayResponse(
                log.getId(),
                log.getMission().getId(),
                log.getMission().getTitle(),
                log.getMission().getDifficulty(),
                log.getTargetDate(),
                log.getStatus(),
                log.getAssignedAt(),
                log.getPopupShownAt(),
                log.getDeadlineAt(),
                log.getPopupShownAt() == null
        );
    }
}
