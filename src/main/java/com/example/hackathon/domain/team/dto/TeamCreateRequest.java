package com.example.hackathon.domain.team.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record TeamCreateRequest(

        @NotNull(message = "사용자 ID는 필수입니다.")
        Long userId,

        @NotBlank(message = "팀 이름은 필수입니다.")
        @Size(max = 10, message = "팀 이름은 10자 이하여야 합니다.")
        String teamName
) {
}
