package com.example.hackathon.domain.user.dto.response;

public record UserCreateResponse(
    Long userId,
    String nickname,
    String status
) {
    public static UserCreateResponse of(Long userId, String nickname, String status) {
        return new UserCreateResponse(userId, nickname, status);
    }
}
