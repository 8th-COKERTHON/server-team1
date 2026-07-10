package com.example.hackathon.domain.image.dto;

import jakarta.validation.constraints.NotBlank;

public record PresignedUrlRequest(

        @NotBlank(message = "파일명은 필수입니다.")
        String fileName,

        @NotBlank(message = "contentType 은 필수입니다.")
        String contentType
) {
}
