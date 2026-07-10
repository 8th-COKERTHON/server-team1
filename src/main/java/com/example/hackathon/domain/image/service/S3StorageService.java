package com.example.hackathon.domain.image.service;

import com.example.hackathon.domain.image.config.S3Properties;
import com.example.hackathon.global.exception.BusinessException;
import com.example.hackathon.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class S3StorageService implements StorageService {

    private static final long MAX_IMAGE_SIZE = 10L * 1024 * 1024;
    private static final Map<String, String> ALLOWED_CONTENT_TYPES = Map.of(
            "image/jpeg", "jpg",
            "image/png", "png",
            "image/webp", "webp"
    );

    private final S3Client s3Client;
    private final S3Properties properties;

    @Override
    public String uploadMissionImage(Long missionLogId, MultipartFile image) {
        String extension = validate(image);
        String key = "mission/%d/%s.%s".formatted(missionLogId, UUID.randomUUID(), extension);
        String bucket = properties.s3().bucket();
        try {
            s3Client.putObject(
                    PutObjectRequest.builder()
                            .bucket(bucket)
                            .key(key)
                            .contentType(image.getContentType())
                            .build(),
                    RequestBody.fromBytes(image.getBytes())
            );
            return "https://%s.s3.%s.amazonaws.com/%s".formatted(bucket, properties.resolvedRegion(), key);
        } catch (IOException | RuntimeException exception) {
            throw new BusinessException(ErrorCode.IMAGE_ERROR_500_UPLOAD_FAILED, exception);
        }
    }

    @Override
    public void delete(String imageUrl) {
        String path = URI.create(imageUrl).getPath();
        String key = path.startsWith("/") ? path.substring(1) : path;
        s3Client.deleteObject(DeleteObjectRequest.builder()
                .bucket(properties.s3().bucket())
                .key(key)
                .build());
    }

    private String validate(MultipartFile image) {
        if (image == null || image.isEmpty()) {
            throw new BusinessException(ErrorCode.IMAGE_ERROR_400_EMPTY_FILE);
        }
        if (image.getSize() > MAX_IMAGE_SIZE) {
            throw new BusinessException(ErrorCode.IMAGE_ERROR_400_SIZE_EXCEEDED);
        }
        String contentType = image.getContentType();
        if (contentType == null) {
            throw new BusinessException(ErrorCode.IMAGE_ERROR_400_INVALID_CONTENT_TYPE);
        }
        String extension = ALLOWED_CONTENT_TYPES.get(contentType);
        if (extension == null) {
            throw new BusinessException(ErrorCode.IMAGE_ERROR_400_INVALID_CONTENT_TYPE);
        }
        return extension;
    }
}
