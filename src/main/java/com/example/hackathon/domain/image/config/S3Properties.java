package com.example.hackathon.domain.image.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * S3 설정. 값은 application.yml → 환경변수(.env) 에서 주입된다.
 * accessKey/secretKey 는 비어 있을 수 있다(로컬에서 실제 발급 안 하고 실행하는 경우).
 */
@ConfigurationProperties(prefix = "aws")
public record S3Properties(
        String region,
        S3 s3
) {
    public record S3(
            String bucket,
            String accessKey,
            String secretKey,
            long presignedUrlExpirationSeconds
    ) {
    }
}
