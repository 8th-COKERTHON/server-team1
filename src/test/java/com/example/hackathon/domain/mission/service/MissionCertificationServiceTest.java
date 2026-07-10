package com.example.hackathon.domain.mission.service;

import com.example.hackathon.domain.image.service.StorageService;
import com.example.hackathon.domain.mission.dto.response.MissionCertificationResponse;
import com.example.hackathon.domain.mission.dto.response.MissionCertificationRetakeResponse;
import com.example.hackathon.domain.mission.entity.DailyMission;
import com.example.hackathon.domain.mission.entity.Difficulty;
import com.example.hackathon.domain.mission.entity.Mission;
import com.example.hackathon.domain.mission.entity.MissionStatus;
import com.example.hackathon.domain.mission.entity.UserMissionLog;
import com.example.hackathon.domain.mission.repository.UserMissionLogRepository;
import com.example.hackathon.domain.user.entity.User;
import com.example.hackathon.domain.user.repository.UserRepository;
import com.example.hackathon.global.exception.BusinessException;
import com.example.hackathon.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MissionCertificationServiceTest {

    private static final String DEVICE_ID = "certification-device";
    private static final String IMAGE_URL = "https://bucket.s3.ap-northeast-2.amazonaws.com/mission/10/new.jpg";
    private static final LocalDate TARGET_DATE = LocalDate.of(2026, 7, 11);

    @Mock UserRepository userRepository;
    @Mock UserMissionLogRepository userMissionLogRepository;
    @Mock StorageService storageService;
    @Mock Clock clock;
    @Mock MissionCertificationTransactionService transactionService;
    @InjectMocks MissionCertificationService service;

    private User user;
    private UserMissionLog log;
    private MockMultipartFile image;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .deviceId(DEVICE_ID)
                .nickname("테스터")
                .detoxStartTime(LocalTime.of(22, 0))
                .detoxEndTime(LocalTime.of(23, 0))
                .build();
        ReflectionTestUtils.setField(user, "id", 1L);

        Mission mission = Mission.builder()
                .title("물 마시기")
                .difficulty(Difficulty.EASY)
                .isActive(true)
                .build();
        DailyMission dailyMission = DailyMission.builder()
                .mission(mission)
                .targetDate(TARGET_DATE)
                .build();
        log = buildLog(user, dailyMission, MissionStatus.ASSIGNED, TARGET_DATE,
                TARGET_DATE.atTime(22, 0), TARGET_DATE.atTime(22, 10));
        image = new MockMultipartFile("image", "proof.jpg", "image/jpeg", new byte[]{1, 2, 3});
    }

    @ParameterizedTest
    @EnumSource(value = MissionStatus.class, names = {"ASSIGNED", "CONFIRMED"})
    void certifyChangesEligibleStatusToSuccess(MissionStatus initialStatus) {
        ReflectionTestUtils.setField(log, "status", initialStatus);
        LocalDateTime now = TARGET_DATE.atTime(22, 5);
        prepare(now, TARGET_DATE, log);
        when(storageService.uploadMissionImage(log.getId(), image)).thenReturn(IMAGE_URL);
        when(transactionService.certifyUploadedImage(user.getId(), TARGET_DATE, IMAGE_URL, now))
                .thenAnswer(invocation -> {
                    log.certify(IMAGE_URL, now);
                    return log;
                });

        MissionCertificationResponse response = service.certify(DEVICE_ID, image);

        assertThat(response.status()).isEqualTo(MissionStatus.SUCCESS);
        assertThat(response.imageUrl()).isEqualTo(IMAGE_URL);
        assertThat(response.completedAt()).isEqualTo(now);
        assertThat(response.canRetake()).isTrue();
        verify(transactionService).certifyUploadedImage(user.getId(), TARGET_DATE, IMAGE_URL, now);
    }

    @Test
    void certifyAfterDeadlinePersistsFailedAndRejectsUpload() {
        LocalDateTime now = TARGET_DATE.atTime(22, 11);
        prepare(now, TARGET_DATE, log);

        assertThatThrownBy(() -> service.certify(DEVICE_ID, image))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.MISSION_ERROR_400_DEADLINE_EXCEEDED);
        assertThat(log.getStatus()).isEqualTo(MissionStatus.FAILED);
        verify(userMissionLogRepository).saveAndFlush(log);
        verifyNoInteractions(storageService);
    }

    @Test
    void certifyRejectsAlreadySuccessfulMission() {
        ReflectionTestUtils.setField(log, "status", MissionStatus.SUCCESS);
        ReflectionTestUtils.setField(log, "imageUrl", "old.jpg");
        prepare(TARGET_DATE.atTime(22, 5), TARGET_DATE, log);

        assertThatThrownBy(() -> service.certify(DEVICE_ID, image))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.MISSION_ERROR_400_ALREADY_CERTIFIED);
    }

    @Test
    void uploadFailureDoesNotChangeMissionStatus() {
        prepare(TARGET_DATE.atTime(22, 5), TARGET_DATE, log);
        when(storageService.uploadMissionImage(log.getId(), image)).thenThrow(new RuntimeException("S3 failure"));

        assertThatThrownBy(() -> service.certify(DEVICE_ID, image)).isInstanceOf(RuntimeException.class);
        assertThat(log.getStatus()).isEqualTo(MissionStatus.ASSIGNED);
        verify(userMissionLogRepository, never()).saveAndFlush(any());
    }

    @Test
    void retakeReplacesOnlyImageAndKeepsCompletion() {
        LocalDateTime completedAt = TARGET_DATE.atTime(22, 4);
        ReflectionTestUtils.setField(log, "status", MissionStatus.SUCCESS);
        ReflectionTestUtils.setField(log, "imageUrl", "https://bucket/old.jpg");
        ReflectionTestUtils.setField(log, "completedAt", completedAt);
        LocalDateTime now = TARGET_DATE.atTime(22, 20);
        LocalDateTime storedUpdatedAt = now.plusSeconds(1);
        prepare(now, TARGET_DATE, log);
        when(storageService.uploadMissionImage(log.getId(), image)).thenReturn(IMAGE_URL);
        when(userMissionLogRepository.saveAndFlush(log)).thenAnswer(invocation -> {
            ReflectionTestUtils.setField(log, "updatedAt", storedUpdatedAt);
            return log;
        });

        MissionCertificationRetakeResponse response = service.retake(DEVICE_ID, image);

        assertThat(response.status()).isEqualTo(MissionStatus.SUCCESS);
        assertThat(response.imageUrl()).isEqualTo(IMAGE_URL);
        assertThat(response.completedAt()).isEqualTo(completedAt);
        assertThat(response.updatedAt()).isEqualTo(storedUpdatedAt);
        verify(storageService).delete("https://bucket/old.jpg");
    }

    @Test
    void retakeAfterDetoxEndIsRejected() {
        ReflectionTestUtils.setField(log, "status", MissionStatus.SUCCESS);
        ReflectionTestUtils.setField(log, "imageUrl", "old.jpg");
        prepare(TARGET_DATE.atTime(23, 0), TARGET_DATE, log);

        assertThatThrownBy(() -> service.retake(DEVICE_ID, image))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.MISSION_ERROR_400_DETOX_ALREADY_ENDED);
    }

    @Test
    void retakeUploadFailureKeepsExistingCertification() {
        LocalDateTime completedAt = TARGET_DATE.atTime(22, 4);
        String oldImageUrl = "https://bucket/old.jpg";
        ReflectionTestUtils.setField(log, "status", MissionStatus.SUCCESS);
        ReflectionTestUtils.setField(log, "imageUrl", oldImageUrl);
        ReflectionTestUtils.setField(log, "completedAt", completedAt);
        prepare(TARGET_DATE.atTime(22, 20), TARGET_DATE, log);
        when(storageService.uploadMissionImage(log.getId(), image)).thenThrow(new RuntimeException("S3 failure"));

        assertThatThrownBy(() -> service.retake(DEVICE_ID, image)).isInstanceOf(RuntimeException.class);
        assertThat(log.getImageUrl()).isEqualTo(oldImageUrl);
        assertThat(log.getCompletedAt()).isEqualTo(completedAt);
        assertThat(log.getStatus()).isEqualTo(MissionStatus.SUCCESS);
        verify(userMissionLogRepository, never()).saveAndFlush(any());
        verify(storageService, never()).delete(anyString());
    }

    @Test
    void retakeSaveFailureDeletesNewlyUploadedImage() {
        ReflectionTestUtils.setField(log, "status", MissionStatus.SUCCESS);
        ReflectionTestUtils.setField(log, "imageUrl", "https://bucket/old.jpg");
        ReflectionTestUtils.setField(log, "completedAt", TARGET_DATE.atTime(22, 4));
        prepare(TARGET_DATE.atTime(22, 20), TARGET_DATE, log);
        when(storageService.uploadMissionImage(log.getId(), image)).thenReturn(IMAGE_URL);
        when(userMissionLogRepository.saveAndFlush(log)).thenThrow(new RuntimeException("DB failure"));

        assertThatThrownBy(() -> service.retake(DEVICE_ID, image)).isInstanceOf(RuntimeException.class);
        verify(storageService).delete(IMAGE_URL);
        verify(storageService, never()).delete("https://bucket/old.jpg");
    }

    @Test
    void overnightRetakeUsesPreviousTargetDateWithinMissionDeadline() {
        user = User.builder()
                .deviceId(DEVICE_ID)
                .nickname("야간")
                .detoxStartTime(LocalTime.of(23, 55))
                .detoxEndTime(LocalTime.of(1, 0))
                .build();
        ReflectionTestUtils.setField(user, "id", 2L);
        ReflectionTestUtils.setField(log, "user", user);
        ReflectionTestUtils.setField(log, "status", MissionStatus.SUCCESS);
        ReflectionTestUtils.setField(log, "imageUrl", "old.jpg");
        LocalDateTime now = TARGET_DATE.plusDays(1).atTime(0, 4);
        prepare(now, TARGET_DATE, log);
        when(storageService.uploadMissionImage(log.getId(), image)).thenReturn(IMAGE_URL);

        MissionCertificationRetakeResponse response = service.retake(DEVICE_ID, image);

        assertThat(response.canRetake()).isTrue();
        verify(userMissionLogRepository).findByUserIdAndTargetDateForUpdate(user.getId(), TARGET_DATE);
    }

    @Test
    void missingTodayMissionIsRejected() {
        setClock(TARGET_DATE.atTime(22, 5));
        when(userRepository.findByDeviceId(DEVICE_ID)).thenReturn(Optional.of(user));
        when(userMissionLogRepository.findByUserIdAndTargetDate(user.getId(), TARGET_DATE))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.certify(DEVICE_ID, image))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.MISSION_ERROR_404_NOT_FOUND);
    }

    private void prepare(LocalDateTime now, LocalDate targetDate, UserMissionLog targetLog) {
        setClock(now);
        when(userRepository.findByDeviceId(DEVICE_ID)).thenReturn(Optional.of(user));
        lenient().when(userMissionLogRepository.findByUserIdAndTargetDate(user.getId(), targetDate))
                .thenReturn(Optional.of(targetLog));
        lenient().when(userMissionLogRepository.findByUserIdAndTargetDateForUpdate(user.getId(), targetDate))
                .thenReturn(Optional.of(targetLog));
    }

    private void setClock(LocalDateTime now) {
        ZoneId zone = ZoneId.of("Asia/Seoul");
        Instant instant = now.atZone(zone).toInstant();
        when(clock.instant()).thenReturn(instant);
        when(clock.getZone()).thenReturn(zone);
    }

    private UserMissionLog buildLog(
            User owner,
            DailyMission dailyMission,
            MissionStatus status,
            LocalDate targetDate,
            LocalDateTime assignedAt,
            LocalDateTime deadlineAt
    ) {
        UserMissionLog result = UserMissionLog.builder()
                .user(owner)
                .dailyMission(dailyMission)
                .targetDate(targetDate)
                .status(status)
                .assignedAt(assignedAt)
                .deadlineAt(deadlineAt)
                .build();
        ReflectionTestUtils.setField(result, "id", 10L);
        return result;
    }
}
