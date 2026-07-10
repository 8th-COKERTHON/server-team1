package com.example.hackathon.domain.mission.service;

import com.example.hackathon.domain.user.entity.User;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;

class MissionTargetDateResolverTest {

    private final User user = User.builder()
            .deviceId("target-date-boundary-device")
            .nickname("경계값")
            .detoxStartTime(LocalTime.of(23, 55))
            .detoxEndTime(LocalTime.of(1, 0))
            .build();

    @Test
    void exactDeadlineStillResolvesToPreviousDate() {
        LocalDate previousDate = LocalDate.of(2026, 7, 11);
        LocalDateTime exactDeadline = previousDate.atTime(23, 55)
                .plusMinutes(MissionTargetDateResolver.DEADLINE_MINUTES);

        assertThat(MissionTargetDateResolver.resolve(user, exactDeadline)).isEqualTo(previousDate);
    }

    @Test
    void afterDeadlineResolvesToCurrentDate() {
        LocalDate previousDate = LocalDate.of(2026, 7, 11);
        LocalDateTime afterDeadline = previousDate.atTime(23, 55)
                .plusMinutes(MissionTargetDateResolver.DEADLINE_MINUTES)
                .plusNanos(1);

        assertThat(MissionTargetDateResolver.resolve(user, afterDeadline))
                .isEqualTo(previousDate.plusDays(1));
    }
}
