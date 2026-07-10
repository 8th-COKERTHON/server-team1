package com.example.hackathon.domain.user.dto;

import com.example.hackathon.domain.user.entity.Provider;
import com.example.hackathon.domain.user.entity.Role;
import com.example.hackathon.domain.user.entity.User;

public record UserResponse(
        Long id,
        String email,
        String nickname,
        Provider provider,
        Role role
) {
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getNickname(),
                user.getProvider(),
                user.getRole()
        );
    }
}
