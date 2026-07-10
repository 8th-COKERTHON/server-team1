package com.example.hackathon.domain.auth.oauth;

import com.example.hackathon.domain.user.entity.Provider;

/**
 * 소셜 제공자(카카오/구글)로부터 받아온 사용자 식별 정보 (공통 포맷).
 */
public record OAuthUserInfo(
        Provider provider,
        String providerId,
        String email,
        String nickname
) {
}
