package com.example.hackathon.domain.image.dto;

public record PresignedUrlResponse(
        String uploadUrl,
        String imageUrl,
        long expiresIn
) {
}
