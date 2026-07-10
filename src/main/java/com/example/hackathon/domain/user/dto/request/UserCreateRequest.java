package com.example.hackathon.domain.user.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UserCreateRequest(
    @NotBlank(message = "디바이스 아이디는 필수값입니다.")
    String deviceId,

    @NotBlank(message = "닉네임은 필수값입니다.")
    @Size(min = 2, max = 5, message = "닉네임은 2자 이상 5자 이하여야 합니다.")
    @Pattern(regexp = "^[a-zA-Z0-9가-힣]*$", message = "닉네임에는 특수문자나 공백이 포함될 수 없습니다.")
    String nickname,

    @Email(message = "올바른 이메일 형식이 아닙니다.")
    String email
) {}
