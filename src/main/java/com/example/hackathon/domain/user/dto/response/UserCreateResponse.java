package com.example.hackathon.domain.user.dto.response;

public record UserCreateResponse(
    Long userId,
    String nickname,
    String status,
    Long selectedTeamId,
    String email
) {
    public static UserCreateResponse of(Long userId, String nickname, String status, Long selectedTeamId, String email) {
        return new UserCreateResponse(userId, nickname, status, selectedTeamId, email);
    }
}
