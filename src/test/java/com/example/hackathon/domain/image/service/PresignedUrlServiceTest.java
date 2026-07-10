package com.example.hackathon.domain.image.service;

import com.example.hackathon.domain.image.config.S3Properties;
import com.example.hackathon.domain.image.dto.PresignedUrlRequest;
import com.example.hackathon.domain.image.dto.PresignedUrlResponse;
import com.example.hackathon.global.exception.BusinessException;
import com.example.hackathon.global.exception.ErrorCode;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * presign 은 네트워크 호출 없이 로컬에서 서명만 하므로, 더미 자격증명으로
 * 실제 S3 없이 URL 형식을 검증할 수 있다. CI 에도 AWS 자격증명이 필요 없다.
 */
class PresignedUrlServiceTest {

    private static final String BUCKET = "test-bucket";
    private static final String REGION = "ap-northeast-2";

    private static S3Presigner presigner;
    private PresignedUrlService service;

    @BeforeAll
    static void setUpPresigner() {
        presigner = S3Presigner.builder()
                .region(Region.of(REGION))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create("dummy-access", "dummy-secret")))
                .build();
    }

    @AfterAll
    static void tearDown() {
        presigner.close();
    }

    PresignedUrlServiceTest() {
        S3Properties props = new S3Properties(REGION,
                new S3Properties.S3(BUCKET, "dummy-access", "dummy-secret", 300));
        this.service = new PresignedUrlService(presigner, props);
    }

    @Test
    @DisplayName("jpeg 요청이면 mission/ 경로에 .jpg 키로 업로드 URL 과 공개 imageUrl 을 발급한다")
    void issue_jpeg() {
        PresignedUrlResponse response = service.issue(
                new PresignedUrlRequest("photo.jpg", "image/jpeg"));

        assertThat(response.uploadUrl()).contains(BUCKET).contains("X-Amz-Signature");
        assertThat(response.imageUrl())
                .startsWith("https://" + BUCKET + ".s3." + REGION + ".amazonaws.com/mission/")
                .endsWith(".jpg");
        assertThat(response.expiresIn()).isEqualTo(300);
        // 업로드 URL 과 공개 URL 은 같은 객체 키를 가리켜야 한다.
        assertThat(response.uploadUrl()).contains(objectKey(response.imageUrl()));
    }

    @Test
    @DisplayName("png 요청이면 .png 확장자로 발급한다")
    void issue_png() {
        PresignedUrlResponse response = service.issue(
                new PresignedUrlRequest("photo.png", "image/png"));

        assertThat(response.imageUrl()).endsWith(".png");
    }

    @Test
    @DisplayName("매 발급마다 객체 키(UUID)가 달라 덮어쓰기가 나지 않는다")
    void issue_uniqueKey() {
        String url1 = service.issue(new PresignedUrlRequest("a.jpg", "image/jpeg")).imageUrl();
        String url2 = service.issue(new PresignedUrlRequest("a.jpg", "image/jpeg")).imageUrl();

        assertThat(url1).isNotEqualTo(url2);
    }

    @Test
    @DisplayName("허용하지 않는 형식이면 IMAGE_ERROR_400_INVALID_CONTENT_TYPE")
    void issue_invalidContentType() {
        assertThatThrownBy(() -> service.issue(new PresignedUrlRequest("x.gif", "image/gif")))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.IMAGE_ERROR_400_INVALID_CONTENT_TYPE);
    }

    private String objectKey(String imageUrl) {
        return imageUrl.substring(imageUrl.indexOf("mission/"));
    }
}
