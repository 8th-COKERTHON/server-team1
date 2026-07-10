package com.example.hackathon.domain.mission.service;

import com.example.hackathon.domain.mission.dto.response.*;
import com.example.hackathon.domain.mission.entity.*;
import com.example.hackathon.domain.mission.repository.DailyMissionRepository;
import com.example.hackathon.domain.mission.repository.MissionRepository;
import com.example.hackathon.domain.mission.repository.UserMissionLogRepository;
import com.example.hackathon.domain.mission.scheduler.MissionScheduler;
import com.example.hackathon.domain.user.entity.User;
import com.example.hackathon.domain.user.repository.UserRepository;
import com.example.hackathon.global.exception.BusinessException;
import com.example.hackathon.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MissionTimerTest {

    @InjectMocks
    private MissionService missionService;

    @Mock
    private UserRepository userRepository;
    @Mock
    private MissionRepository missionRepository;
    @Mock
    private DailyMissionRepository dailyMissionRepository;
    @Mock
    private UserMissionLogRepository userMissionLogRepository;
    @Mock
    private DailyMissionTransactionService transactionService;
    @Mock
    private Clock clock;

    private final String deviceId = "test-device-id";
    private final LocalDate today = LocalDate.of(2026, 7, 10);
    private final LocalTime detoxStart = LocalTime.of(22, 0);
    private final LocalTime detoxEnd = LocalTime.of(23, 0);

    private User user;
    private Mission mission;
    private DailyMission dailyMission;
    private UserMissionLog userMissionLog;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .deviceId(deviceId)
                .nickname("테스터")
                .detoxStartTime(detoxStart)
                .detoxEndTime(detoxEnd)
                .build();
        ReflectionTestUtils.setField(user, "id", 1L);

        mission = Mission.builder()
                .title("물 한 잔 마시기")
                .difficulty(Difficulty.EASY)
                .isActive(true)
                .build();
        ReflectionTestUtils.setField(mission, "id", 100L);

        dailyMission = DailyMission.builder()
                .mission(mission)
                .targetDate(today)
                .build();
        ReflectionTestUtils.setField(dailyMission, "id", 200L);

        LocalDateTime assignedAt = LocalDateTime.of(today, detoxStart);
        LocalDateTime deadlineAt = assignedAt.plusMinutes(10);

        userMissionLog = UserMissionLog.builder()
                .user(user)
                .dailyMission(dailyMission)
                .targetDate(today)
                .status(MissionStatus.ASSIGNED)
                .assignedAt(assignedAt)
                .deadlineAt(deadlineAt)
                .build();
        ReflectionTestUtils.setField(userMissionLog, "id", 500L);
    }

    private void setClockTime(LocalDateTime dateTime) {
        Instant instant = dateTime.atZone(ZoneId.of("Asia/Seoul")).toInstant();
        when(clock.instant()).thenReturn(instant);
        when(clock.getZone()).thenReturn(ZoneId.of("Asia/Seoul"));
    }

    @Test
    @DisplayName("1. 디톡스 시작 시간 전에는 popupRequired가 false다.")
    void popupRequiredFalseBeforeStart() {
        // given
        LocalDateTime beforeTime = LocalDateTime.of(today, detoxStart.minusMinutes(5));
        setClockTime(beforeTime);

        when(userRepository.findByDeviceId(deviceId)).thenReturn(Optional.of(user));
        
        // when & then
        assertThatThrownBy(() -> missionService.getTodayMissionStatus(deviceId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(ErrorCode.MISSION_ERROR_400_BEFORE_DETOX_START.getMessage());
    }

    @Test
    @DisplayName("2. 디톡스 시작 시간 이후이고 deadline_at 이전이면 popupRequired가 true다.")
    void popupRequiredTrueDuringTimer() {
        // given
        LocalDateTime duringTime = LocalDateTime.of(today, detoxStart.plusMinutes(5));
        setClockTime(duringTime);

        when(userRepository.findByDeviceId(deviceId)).thenReturn(Optional.of(user));
        when(userMissionLogRepository.findByUserIdAndTargetDate(user.getId(), today))
                .thenReturn(Optional.of(userMissionLog));

        // when
        MissionTodayStatusResponse response = missionService.getTodayMissionStatus(deviceId);

        // then
        assertThat(response.popupRequired()).isTrue();
        assertThat(response.status()).isEqualTo(MissionStatus.ASSIGNED);
        assertThat(response.remainingSeconds()).isEqualTo(300);
    }

    @Test
    @DisplayName("3. popup_shown_at이 이미 있어도 제한 시간 전에는 popupRequired가 true다.")
    void popupRequiredTrueEvenIfPopupShownExists() {
        // given
        LocalDateTime duringTime = LocalDateTime.of(today, detoxStart.plusMinutes(5));
        setClockTime(duringTime);

        ReflectionTestUtils.setField(userMissionLog, "popupShownAt", LocalDateTime.of(today, detoxStart.plusMinutes(2)));

        when(userRepository.findByDeviceId(deviceId)).thenReturn(Optional.of(user));
        when(userMissionLogRepository.findByUserIdAndTargetDate(user.getId(), today))
                .thenReturn(Optional.of(userMissionLog));

        // when
        MissionTodayStatusResponse response = missionService.getTodayMissionStatus(deviceId);

        // then
        assertThat(response.popupRequired()).isTrue();
    }

    @Test
    @DisplayName("4. SUCCESS 상태에서는 popupRequired가 false다.")
    void popupRequiredFalseWhenSuccess() {
        // given
        LocalDateTime duringTime = LocalDateTime.of(today, detoxStart.plusMinutes(5));
        setClockTime(duringTime);

        ReflectionTestUtils.setField(userMissionLog, "status", MissionStatus.SUCCESS);

        when(userRepository.findByDeviceId(deviceId)).thenReturn(Optional.of(user));
        when(userMissionLogRepository.findByUserIdAndTargetDate(user.getId(), today))
                .thenReturn(Optional.of(userMissionLog));

        // when
        MissionTodayStatusResponse response = missionService.getTodayMissionStatus(deviceId);

        // then
        assertThat(response.popupRequired()).isFalse();
    }

    @Test
    @DisplayName("5. FAILED 상태에서는 popupRequired가 false다.")
    void popupRequiredFalseWhenFailed() {
        // given
        LocalDateTime duringTime = LocalDateTime.of(today, detoxStart.plusMinutes(5));
        setClockTime(duringTime);

        ReflectionTestUtils.setField(userMissionLog, "status", MissionStatus.FAILED);

        when(userRepository.findByDeviceId(deviceId)).thenReturn(Optional.of(user));
        when(userMissionLogRepository.findByUserIdAndTargetDate(user.getId(), today))
                .thenReturn(Optional.of(userMissionLog));

        // when
        MissionTodayStatusResponse response = missionService.getTodayMissionStatus(deviceId);

        // then
        assertThat(response.popupRequired()).isFalse();
    }

    @Test
    @DisplayName("6. popup API 최초 호출 시 popup_shown_at이 저장된다.")
    void popupFirstCallSavesPopupShownAt() {
        // given
        LocalDateTime showTime = LocalDateTime.of(today, detoxStart.plusMinutes(3));
        setClockTime(showTime);

        when(userRepository.findByDeviceId(deviceId)).thenReturn(Optional.of(user));
        when(userMissionLogRepository.findByUserIdAndTargetDate(user.getId(), today))
                .thenReturn(Optional.of(userMissionLog));

        // when
        MissionPopupResponse response = missionService.recordPopupShown(deviceId);

        // then
        assertThat(response.popupShownAt()).isEqualTo(showTime);
        verify(userMissionLogRepository, times(1)).save(userMissionLog);
    }

    @Test
    @DisplayName("7. popup API를 다시 호출해도 최초 popup_shown_at이 유지된다.")
    void popupSecondCallKeepsOriginalPopupShownAt() {
        // given
        LocalDateTime firstShowTime = LocalDateTime.of(today, detoxStart.plusMinutes(3));
        LocalDateTime secondShowTime = LocalDateTime.of(today, detoxStart.plusMinutes(5));
        setClockTime(secondShowTime);

        ReflectionTestUtils.setField(userMissionLog, "popupShownAt", firstShowTime);

        when(userRepository.findByDeviceId(deviceId)).thenReturn(Optional.of(user));
        when(userMissionLogRepository.findByUserIdAndTargetDate(user.getId(), today))
                .thenReturn(Optional.of(userMissionLog));

        // when
        MissionPopupResponse response = missionService.recordPopupShown(deviceId);

        // then
        assertThat(response.popupShownAt()).isEqualTo(firstShowTime);
        verify(userMissionLogRepository, times(1)).save(userMissionLog);
    }

    @Test
    @DisplayName("8. 팝업 노출 시 deadline_at이 변경되지 않는다.")
    void popupCallDoesNotChangeDeadlineAt() {
        // given
        LocalDateTime showTime = LocalDateTime.of(today, detoxStart.plusMinutes(3));
        setClockTime(showTime);

        LocalDateTime originalDeadline = userMissionLog.getDeadlineAt();

        when(userRepository.findByDeviceId(deviceId)).thenReturn(Optional.of(user));
        when(userMissionLogRepository.findByUserIdAndTargetDate(user.getId(), today))
                .thenReturn(Optional.of(userMissionLog));

        // when
        MissionPopupResponse response = missionService.recordPopupShown(deviceId);

        // then
        assertThat(response.deadlineAt()).isEqualTo(originalDeadline);
    }

    @Test
    @DisplayName("9. confirm API 호출 시 ASSIGNED가 CONFIRMED로 변경된다.")
    void confirmChangesAssignedToConfirmed() {
        // given
        LocalDateTime confirmTime = LocalDateTime.of(today, detoxStart.plusMinutes(3));
        setClockTime(confirmTime);

        when(userRepository.findByDeviceId(deviceId)).thenReturn(Optional.of(user));
        when(userMissionLogRepository.findByUserIdAndTargetDate(user.getId(), today))
                .thenReturn(Optional.of(userMissionLog));

        // when
        MissionConfirmResponse response = missionService.confirmMission(deviceId);

        // then
        assertThat(response.status()).isEqualTo(MissionStatus.CONFIRMED);
        verify(userMissionLogRepository, times(1)).save(userMissionLog);
    }

    @Test
    @DisplayName("10. confirm API 재호출 시 기존 CONFIRMED 상태를 반환한다.")
    void confirmSecondCallReturnsSameConfirmed() {
        // given
        LocalDateTime confirmTime = LocalDateTime.of(today, detoxStart.plusMinutes(3));
        setClockTime(confirmTime);

        ReflectionTestUtils.setField(userMissionLog, "status", MissionStatus.CONFIRMED);

        when(userRepository.findByDeviceId(deviceId)).thenReturn(Optional.of(user));
        when(userMissionLogRepository.findByUserIdAndTargetDate(user.getId(), today))
                .thenReturn(Optional.of(userMissionLog));

        // when
        MissionConfirmResponse response = missionService.confirmMission(deviceId);

        // then
        assertThat(response.status()).isEqualTo(MissionStatus.CONFIRMED);
    }

    @Test
    @DisplayName("11. confirm 시 타이머가 초기화되지 않는다.")
    void confirmDoesNotResetTimer() {
        // given
        LocalDateTime confirmTime = LocalDateTime.of(today, detoxStart.plusMinutes(3));
        setClockTime(confirmTime);

        LocalDateTime originalDeadline = userMissionLog.getDeadlineAt();

        when(userRepository.findByDeviceId(deviceId)).thenReturn(Optional.of(user));
        when(userMissionLogRepository.findByUserIdAndTargetDate(user.getId(), today))
                .thenReturn(Optional.of(userMissionLog));

        // when
        MissionConfirmResponse response = missionService.confirmMission(deviceId);

        // then
        assertThat(response.deadlineAt()).isEqualTo(originalDeadline);
        assertThat(response.remainingSeconds()).isEqualTo(420);
    }

    @Test
    @DisplayName("12. deadline_at 이후 상태 조회 시 FAILED로 변경된다.")
    void getStatusAfterDeadlineChangesToFailed() {
        // given
        LocalDateTime expiredTime = LocalDateTime.of(today, detoxStart.plusMinutes(11));
        setClockTime(expiredTime);

        when(userRepository.findByDeviceId(deviceId)).thenReturn(Optional.of(user));
        when(userMissionLogRepository.findByUserIdAndTargetDate(user.getId(), today))
                .thenReturn(Optional.of(userMissionLog));

        // when
        MissionTodayStatusResponse response = missionService.getTodayMissionStatus(deviceId);

        // then
        assertThat(response.status()).isEqualTo(MissionStatus.FAILED);
        assertThat(response.expired()).isTrue();
        assertThat(response.remainingSeconds()).isZero();
        verify(userMissionLogRepository, times(1)).save(userMissionLog);
    }

    @Test
    @DisplayName("13. deadline_at 이후 popup API 호출 시 FAILED 처리 및 예외가 발생한다.")
    void popupCallAfterDeadlineThrowsException() {
        // given
        LocalDateTime expiredTime = LocalDateTime.of(today, detoxStart.plusMinutes(11));
        setClockTime(expiredTime);

        when(userRepository.findByDeviceId(deviceId)).thenReturn(Optional.of(user));
        when(userMissionLogRepository.findByUserIdAndTargetDate(user.getId(), today))
                .thenReturn(Optional.of(userMissionLog));

        // when & then
        assertThatThrownBy(() -> missionService.recordPopupShown(deviceId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(ErrorCode.MISSION_ERROR_400_ALREADY_FAILED.getMessage());
        assertThat(userMissionLog.getStatus()).isEqualTo(MissionStatus.FAILED);
    }

    @Test
    @DisplayName("14. deadline_at 이후 confirm API 호출 시 FAILED 처리 및 예외가 발생한다.")
    void confirmCallAfterDeadlineThrowsException() {
        // given
        LocalDateTime expiredTime = LocalDateTime.of(today, detoxStart.plusMinutes(11));
        setClockTime(expiredTime);

        when(userRepository.findByDeviceId(deviceId)).thenReturn(Optional.of(user));
        when(userMissionLogRepository.findByUserIdAndTargetDate(user.getId(), today))
                .thenReturn(Optional.of(userMissionLog));

        // when & then
        assertThatThrownBy(() -> missionService.confirmMission(deviceId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(ErrorCode.MISSION_ERROR_400_ALREADY_FAILED.getMessage());
        assertThat(userMissionLog.getStatus()).isEqualTo(MissionStatus.FAILED);
    }

    @ParameterizedTest(name = "15. 스케줄러가 만료된 {0} 미션을 FAILED 처리 쿼리에 전달한다.")
    @EnumSource(value = MissionStatus.class, names = {"ASSIGNED", "CONFIRMED"})
    void schedulerExpiresEligibleMission(MissionStatus status) {
        // given
        LocalDateTime expiredTime = LocalDateTime.of(today, detoxStart.plusMinutes(11));
        Instant instant = expiredTime.atZone(ZoneId.of("Asia/Seoul")).toInstant();
        when(clock.instant()).thenReturn(instant);
        when(clock.getZone()).thenReturn(ZoneId.of("Asia/Seoul"));
        UserMissionLogRepository mockRepo = mock(UserMissionLogRepository.class);
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(jdbcTemplate.queryForObject(anyString(), eq(Boolean.class), anyLong())).thenReturn(true);
        MissionScheduler scheduler = new MissionScheduler(mockRepo, clock, jdbcTemplate);

        // when
        scheduler.expireMissions();

        // then
        verify(mockRepo).updateExpiredMissions(
                expiredTime,
                MissionStatus.FAILED,
                MissionStatus.ASSIGNED,
                MissionStatus.CONFIRMED
        );
    }

    @Test
    @DisplayName("18. 자정을 넘기는 deadline_at이 올바르게 계산된다.")
    void midnightDeadlineCorrectlyCalculated() {
        // given
        User midnightUser = User.builder()
                .deviceId(deviceId)
                .nickname("자정유저")
                .detoxStartTime(LocalTime.of(23, 55))
                .detoxEndTime(LocalTime.of(3, 0))
                .build();
        ReflectionTestUtils.setField(midnightUser, "id", 2L);

        LocalDateTime now = LocalDateTime.of(today, LocalTime.of(23, 58));
        setClockTime(now);

        when(userRepository.findByDeviceId(deviceId)).thenReturn(Optional.of(midnightUser));
        when(userMissionLogRepository.findByUserIdAndTargetDate(midnightUser.getId(), today))
                .thenReturn(Optional.of(UserMissionLog.builder()
                        .user(midnightUser)
                        .dailyMission(dailyMission)
                        .targetDate(today)
                        .status(MissionStatus.ASSIGNED)
                        .assignedAt(LocalDateTime.of(today, LocalTime.of(23, 55)))
                        .deadlineAt(LocalDateTime.of(today, LocalTime.of(23, 55)).plusMinutes(10))
                        .build()));

        // when
        MissionTodayStatusResponse response = missionService.getTodayMissionStatus(deviceId);

        // then
        assertThat(response.deadlineAt()).isEqualTo(LocalDateTime.of(today.plusDays(1), LocalTime.of(0, 5)));
        assertThat(response.remainingSeconds()).isEqualTo(420);
    }

    @Test
    @DisplayName("19. remainingSeconds가 음수가 되지 않고 최소 0으로 반환된다.")
    void remainingSecondsMinZero() {
        // given
        LocalDateTime expiredTime = LocalDateTime.of(today, detoxStart.plusMinutes(12));
        setClockTime(expiredTime);

        when(userRepository.findByDeviceId(deviceId)).thenReturn(Optional.of(user));
        when(userMissionLogRepository.findByUserIdAndTargetDate(user.getId(), today))
                .thenReturn(Optional.of(userMissionLog));

        // when
        MissionTodayStatusResponse response = missionService.getTodayMissionStatus(deviceId);

        // then
        assertThat(response.remainingSeconds()).isZero();
    }

    @Test
    @DisplayName("20. 여러 번 상태 조회해도 동일한 미션 로그가 유지된다.")
    void multipleStatusCallsReturnsSameLog() {
        // given
        LocalDateTime now = LocalDateTime.of(today, detoxStart.plusMinutes(5));
        setClockTime(now);

        when(userRepository.findByDeviceId(deviceId)).thenReturn(Optional.of(user));
        when(userMissionLogRepository.findByUserIdAndTargetDate(user.getId(), today))
                .thenReturn(Optional.of(userMissionLog));

        // when
        MissionTodayStatusResponse response1 = missionService.getTodayMissionStatus(deviceId);
        MissionTodayStatusResponse response2 = missionService.getTodayMissionStatus(deviceId);

        // then
        assertThat(response1.missionLogId()).isEqualTo(response2.missionLogId());
        verify(userMissionLogRepository, times(2)).findByUserIdAndTargetDate(user.getId(), today);
    }
}
