package com.example.hackathon.domain.user.dto.request;

import jakarta.validation.constraints.NotNull;

public record UserNotificationRequest(
    @NotNull(message = "알림 설정값은 필수입니다.")
    Boolean enabled
) {}
