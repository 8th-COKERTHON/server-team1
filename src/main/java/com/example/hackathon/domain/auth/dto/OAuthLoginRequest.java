package com.example.hackathon.domain.auth.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 프론트가 카카오/구글에서 받은 인가코드(code)를 백엔드로 전달.
 * redirectUri 는 프론트가 인가코드를 발급받을 때 쓴 값과 동일해야 함(생략 시 서버 기본값 사용).
 */
public record OAuthLoginRequest(
        @NotBlank String code,
        String redirectUri
) {
}
