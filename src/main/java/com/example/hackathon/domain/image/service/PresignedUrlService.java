package com.example.hackathon.domain.image.service;

import com.example.hackathon.domain.image.config.S3Properties;
import com.example.hackathon.domain.image.dto.PresignedUrlRequest;
import com.example.hackathon.domain.image.dto.PresignedUrlResponse;
import com.example.hackathon.global.exception.BusinessException;
import com.example.hackathon.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

/**
 * 미션 인증 이미지 업로드용 presigned URL 발급.
 *
 * 클라이언트는 발급받은 uploadUrl 로 S3 에 직접 PUT 하고,
 * 업로드가 끝나면 imageUrl 을 미션 인증 API 로 넘긴다.
 * 이 서비스는 미션 로그 존재 여부를 검증하지 않는다(그건 인증 API 몫).
 */
@Service
@RequiredArgsConstructor
public class PresignedUrlService {

    private static final String KEY_PREFIX = "mission/";

    /** 허용 이미지 형식 → 파일 확장자. */
    private static final Map<String, String> ALLOWED_CONTENT_TYPES = Map.of(
            "image/jpeg", "jpg",
            "image/png", "png"
    );

    private final S3Presigner s3Presigner;
    private final S3Properties s3Properties;

    public PresignedUrlResponse issue(PresignedUrlRequest request) {
        String extension = ALLOWED_CONTENT_TYPES.get(request.contentType());
        if (extension == null) {
            throw new BusinessException(ErrorCode.IMAGE_ERROR_400_INVALID_CONTENT_TYPE);
        }

        String bucket = s3Properties.s3().bucket();
        String key = KEY_PREFIX + UUID.randomUUID() + "." + extension;
        long expireSeconds = s3Properties.s3().presignedUrlExpirationSeconds();

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType(request.contentType())
                .build();

        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofSeconds(expireSeconds))
                .putObjectRequest(putObjectRequest)
                .build();

        try {
            PresignedPutObjectRequest presigned = s3Presigner.presignPutObject(presignRequest);
            String imageUrl = toPublicUrl(bucket, key);
            return new PresignedUrlResponse(presigned.url().toString(), imageUrl, expireSeconds);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.IMAGE_ERROR_500_PRESIGNED_URL_FAILED);
        }
    }

    /** 버킷이 공개 읽기이므로 업로드 완료 후 이 주소로 바로 접근된다. */
    private String toPublicUrl(String bucket, String key) {
        return "https://%s.s3.%s.amazonaws.com/%s".formatted(bucket, s3Properties.region(), key);
    }
}
