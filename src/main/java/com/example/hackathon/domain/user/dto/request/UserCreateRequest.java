package com.example.hackathon.domain.user.dto.request;

public record UserCreateRequest(
    String deviceId,
    String nickname
) {}
