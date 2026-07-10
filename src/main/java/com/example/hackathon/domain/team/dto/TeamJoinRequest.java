package com.example.hackathon.domain.team.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record TeamJoinRequest(

        @NotNull(message = "사용자 ID는 필수입니다.")
        Long userId,

        @NotBlank(message = "초대코드는 필수입니다.")
        String inviteCode
) {
}
