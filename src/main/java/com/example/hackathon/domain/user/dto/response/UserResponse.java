package com.example.hackathon.domain.user.dto.response;

public record UserResponse(
    Long id,
    String nickname,
    String email,
    boolean emailNotificationEnabled
) {
    public static UserResponse of(Long id, String nickname, String email, boolean emailNotificationEnabled) {
        return new UserResponse(id, nickname, email, emailNotificationEnabled);
    }
}
