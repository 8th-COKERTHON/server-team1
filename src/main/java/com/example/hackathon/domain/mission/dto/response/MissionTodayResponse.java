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
    public static MissionTodayResponse from(UserMissionLog log, java.time.LocalDateTime now) {
        boolean popupRequired = log.isPopupRequired(now);

        return new MissionTodayResponse(
                log.getId(),
                log.getDailyMission().getMission().getId(),
                log.getDailyMission().getMission().getTitle(),
                log.getDailyMission().getMission().getDifficulty(),
                log.getTargetDate(),
                log.getStatus(),
                log.getAssignedAt(),
                log.getPopupShownAt(),
                log.getDeadlineAt(),
                popupRequired
        );
    }
}
