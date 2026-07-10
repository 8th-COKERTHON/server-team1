package com.example.hackathon.domain.user.dto;

public record UserCreateRequest(
    String deviceId, 
    String nickname
) {}