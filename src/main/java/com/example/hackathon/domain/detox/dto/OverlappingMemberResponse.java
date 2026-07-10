package com.example.hackathon.domain.detox.dto;

import java.time.LocalDateTime;
import java.time.LocalTime;

public record OverlappingMemberResponse(
        Long userId,
        String nickname,
        LocalTime detoxStartTime,
        LocalTime detoxEndTime,
        LocalDateTime overlapStartDateTime,
        LocalDateTime overlapEndDateTime
) {
}
