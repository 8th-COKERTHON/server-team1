package com.example.hackathon.domain.image.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.S3Client;

@Configuration
@EnableConfigurationProperties(S3Properties.class)
public class S3Config {

    private final S3Properties properties;

    public S3Config(S3Properties properties) {
        this.properties = properties;
    }

    /**
     * presigned URL 을 만드는 서명기. 실제 S3 를 호출하지 않고 로컬에서 서명만 한다.
     *
     * access-key/secret-key 가 주입되면 그 자격증명을 쓰고, 비어 있으면
     * 기본 자격증명 체인(환경변수·EC2 IAM 역할 등)에 위임한다.
     */
    // region 이 비면 서울로 폴백한다. 설정 하나 빠졌다고 앱 전체(와 모든 테스트)가
    // 부팅에 실패하는 것을 막는다. presign 은 어차피 실제 발급 시점에만 자격증명을 검증한다.
    private static final String DEFAULT_REGION = "ap-northeast-2";

    @Bean
    public S3Presigner s3Presigner() {
        String region = (properties.region() == null || properties.region().isBlank())
                ? DEFAULT_REGION
                : properties.region();
        return S3Presigner.builder()
                .region(Region.of(region))
                .credentialsProvider(credentialsProvider())
                .build();
    }

    @Bean
    public S3Client s3Client() {
        String region = (properties.region() == null || properties.region().isBlank())
                ? DEFAULT_REGION
                : properties.region();
        return S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(credentialsProvider())
                .build();
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
