package com.example.hackathon.domain.image.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

@Configuration
@EnableConfigurationProperties(S3Properties.class)
public class S3Config {

    private final S3Properties properties;

    public S3Config(S3Properties properties) {
        this.properties = properties;
    }

    /**
     * S3 업로드용 클라이언트. 미션 인증 사진 업로드에 쓰인다.
     *
     * access-key/secret-key 가 주입되면 그 자격증명을 쓰고, 비어 있으면
     * 기본 자격증명 체인(환경변수·EC2 IAM 역할 등)에 위임한다.
     */
    @Bean
    public S3Client s3Client() {
        return S3Client.builder()
                .region(resolveRegion())
                .credentialsProvider(credentialsProvider())
                .build();
    }

    private Region resolveRegion() {
        return Region.of(properties.resolvedRegion());
    }

    private AwsCredentialsProvider credentialsProvider() {
        S3Properties.S3 s3 = properties.s3();
        if (s3.accessKey() != null && !s3.accessKey().isBlank()
                && s3.secretKey() != null && !s3.secretKey().isBlank()) {
            return StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(s3.accessKey(), s3.secretKey()));
        }
        return DefaultCredentialsProvider.create();
    }
}
