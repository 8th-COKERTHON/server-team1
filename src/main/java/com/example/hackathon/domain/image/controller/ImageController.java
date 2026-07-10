package com.example.hackathon.domain.image.controller;

import com.example.hackathon.domain.image.dto.PresignedUrlRequest;
import com.example.hackathon.domain.image.dto.PresignedUrlResponse;
import com.example.hackathon.domain.image.service.PresignedUrlService;
import com.example.hackathon.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Image", description = "이미지 업로드 (S3 Presigned URL)")
@RestController
@RequestMapping("/api/images")
@RequiredArgsConstructor
public class ImageController {

    private final PresignedUrlService presignedUrlService;

    @Operation(summary = "업로드 URL 발급",
            description = "S3 에 직접 PUT 할 presigned URL 을 발급한다. 업로드 후 imageUrl 을 미션 인증 API 에 전달한다.")
    @PostMapping("/presigned-url")
    public ResponseEntity<ApiResponse<PresignedUrlResponse>> issue(@Valid @RequestBody PresignedUrlRequest request) {
        PresignedUrlResponse response = presignedUrlService.issue(request);
        return ResponseEntity.ok(ApiResponse.ok("업로드 URL이 발급되었습니다.", response));
    }
}
