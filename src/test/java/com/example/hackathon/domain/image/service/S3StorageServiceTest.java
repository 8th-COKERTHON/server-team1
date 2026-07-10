package com.example.hackathon.domain.image.service;

import com.example.hackathon.domain.image.config.S3Properties;
import com.example.hackathon.global.exception.BusinessException;
import com.example.hackathon.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class S3StorageServiceTest {

    @Mock
    private S3Client s3Client;

    private S3StorageService storageService;

    @BeforeEach
    void setUp() {
        S3Properties properties = new S3Properties(
                "ap-northeast-2",
                new S3Properties.S3("test-bucket", "access", "secret", 300)
        );
        storageService = new S3StorageService(s3Client, properties);
    }

    @Test
    void emptyImageIsRejected() {
        MockMultipartFile image = new MockMultipartFile("image", "empty.jpg", "image/jpeg", new byte[0]);

        assertError(image, ErrorCode.IMAGE_ERROR_400_EMPTY_FILE);
    }

    @Test
    void nonImageContentTypeIsRejected() {
        MockMultipartFile image = new MockMultipartFile("image", "proof.txt", "text/plain", new byte[]{1});

        assertError(image, ErrorCode.IMAGE_ERROR_400_INVALID_CONTENT_TYPE);
    }

    @Test
    void nullContentTypeIsRejected() {
        MockMultipartFile image = new MockMultipartFile("image", "proof", null, new byte[]{1});

        assertError(image, ErrorCode.IMAGE_ERROR_400_INVALID_CONTENT_TYPE);
    }

    @Test
    void imageLargerThanTenMegabytesIsRejected() {
        MockMultipartFile image = new MockMultipartFile(
                "image", "large.webp", "image/webp", new byte[10 * 1024 * 1024 + 1]);

        assertError(image, ErrorCode.IMAGE_ERROR_400_SIZE_EXCEEDED);
    }

    @Test
    void uploadReturnsUrlWithConfiguredRegion() {
        when(s3Client.putObject(any(PutObjectRequest.class), any(software.amazon.awssdk.core.sync.RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().build());
        MockMultipartFile image = new MockMultipartFile("image", "proof.jpg", "image/jpeg", new byte[]{1});

        String imageUrl = storageService.uploadMissionImage(15L, image);

        assertThat(imageUrl).startsWith("https://test-bucket.s3.ap-northeast-2.amazonaws.com/mission/15/")
                .endsWith(".jpg");
    }

    @Test
    void uploadReturnsUrlWithDefaultRegionWhenRegionIsBlank() {
        storageService = new S3StorageService(s3Client, new S3Properties(
                " ", new S3Properties.S3("test-bucket", "access", "secret", 300)));
        when(s3Client.putObject(any(PutObjectRequest.class), any(software.amazon.awssdk.core.sync.RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().build());
        MockMultipartFile image = new MockMultipartFile("image", "proof.png", "image/png", new byte[]{1});

        String imageUrl = storageService.uploadMissionImage(15L, image);

        assertThat(imageUrl).startsWith("https://test-bucket.s3.ap-northeast-2.amazonaws.com/mission/15/")
                .endsWith(".png");
    }

    @Test
    void deleteExtractsObjectKeyFromImageUrl() {
        storageService.delete("https://test-bucket.s3.ap-northeast-2.amazonaws.com/mission/15/photo.jpg");

        ArgumentCaptor<DeleteObjectRequest> requestCaptor = ArgumentCaptor.forClass(DeleteObjectRequest.class);
        verify(s3Client).deleteObject(requestCaptor.capture());
        assertThat(requestCaptor.getValue().bucket()).isEqualTo("test-bucket");
        assertThat(requestCaptor.getValue().key()).isEqualTo("mission/15/photo.jpg");
    }

    private void assertError(MockMultipartFile image, ErrorCode errorCode) {
        assertThatThrownBy(() -> storageService.uploadMissionImage(1L, image))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(errorCode);
        verifyNoInteractions(s3Client);
    }
}
