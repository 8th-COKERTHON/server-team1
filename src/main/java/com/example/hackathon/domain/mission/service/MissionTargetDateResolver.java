package com.example.hackathon.domain.mission.service;

import com.example.hackathon.domain.user.entity.User;

import java.time.LocalDate;
import java.time.LocalDateTime;

final class MissionTargetDateResolver {

    static final long DEADLINE_MINUTES = 10;

    private MissionTargetDateResolver() {
    }

    static LocalDate resolve(User user, LocalDateTime now) {
        LocalDate today = now.toLocalDate();
        LocalDateTime yesterdayDeadline = LocalDateTime
                .of(today.minusDays(1), user.getDetoxStartTime())
                .plusMinutes(DEADLINE_MINUTES);
        return now.isAfter(yesterdayDeadline) ? today : today.minusDays(1);
    }
}
