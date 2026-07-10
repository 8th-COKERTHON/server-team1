package com.example.hackathon.domain.mission.service;

import com.example.hackathon.domain.mission.dto.response.MissionTodayResponse;
import com.example.hackathon.domain.mission.entity.Difficulty;
import com.example.hackathon.domain.mission.entity.Mission;
import com.example.hackathon.domain.mission.entity.MissionStatus;
import com.example.hackathon.domain.mission.entity.UserMissionLog;
import com.example.hackathon.domain.mission.repository.MissionRepository;
import com.example.hackathon.domain.mission.repository.UserMissionLogRepository;
import com.example.hackathon.domain.user.entity.User;
import com.example.hackathon.domain.user.repository.UserRepository;
import com.example.hackathon.global.exception.BusinessException;
import com.example.hackathon.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MissionServiceTest {

    @InjectMocks
    private MissionService missionService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private MissionRepository missionRepository;

    @Mock
    private UserMissionLogRepository userMissionLogRepository;

    private final String deviceId = "test-device-id";
    private final LocalDate today = LocalDate.of(2026, 7, 10);
    private final LocalTime detoxStart = LocalTime.of(22, 0);
    private final LocalTime detoxEnd = LocalTime.of(23, 0);

    private User user;
    private Mission mission;

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
                .build();
        ReflectionTestUtils.setField(mission, "id", 100L);
    }

    @Nested
    @DisplayName("오늘의 미션 조회 및 생성 테스트")
    class GetOrCreateTodayMission {

        @Test
        @DisplayName("존재하지 않는 사용자일 경우 404 예외를 발생시킨다.")
        void userNotFound() {
            // given
            when(userRepository.findByDeviceId(deviceId)).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> missionService.getOrCreateTodayMission(deviceId, today, LocalDateTime.now()))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining(ErrorCode.USER_ERROR_404_NOT_FOUND.getMessage());
        }

        @Test
        @DisplayName("디톡스 시작/종료 시간이 설정되지 않았을 경우 400 예외를 발생시킨다.")
        void detoxTimeNotSet() {
            // given
            User invalidUser = User.builder()
                    .deviceId(deviceId)
                    .nickname("테스터")
                    .detoxStartTime(null)
                    .detoxEndTime(null)
                    .build();
            ReflectionTestUtils.setField(invalidUser, "id", 1L);
            when(userRepository.findByDeviceId(deviceId)).thenReturn(Optional.of(invalidUser));

            // when & then
            assertThatThrownBy(() -> missionService.getOrCreateTodayMission(deviceId, today, LocalDateTime.of(today, LocalTime.of(22, 0))))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining(ErrorCode.MISSION_ERROR_400_DETOX_TIME_NOT_SET.getMessage());
        }

        @Test
        @DisplayName("현재 시간이 디톡스 시작 시각 이전일 경우 400 예외를 발생시킨다.")
        void beforeDetoxStart() {
            // given
            when(userRepository.findByDeviceId(deviceId)).thenReturn(Optional.of(user));

            LocalDateTime beforeTime = LocalDateTime.of(today, detoxStart.minusMinutes(1));

            // when & then
            assertThatThrownBy(() -> missionService.getOrCreateTodayMission(deviceId, today, beforeTime))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining(ErrorCode.MISSION_ERROR_400_BEFORE_DETOX_START.getMessage());
        }

        @Test
        @DisplayName("오늘 배정된 미션 로그가 이미 존재할 경우 새로 생성하지 않고 기존 로그를 반환한다.")
        void alreadyAssigned() {
            // given
            when(userRepository.findByDeviceId(deviceId)).thenReturn(Optional.of(user));

            LocalDateTime assignedAt = LocalDateTime.of(today, detoxStart);
            UserMissionLog existingLog = UserMissionLog.builder()
                    .user(user)
                    .mission(mission)
                    .targetDate(today)
                    .status(MissionStatus.PENDING)
                    .assignedAt(assignedAt)
                    .deadlineAt(assignedAt.plusMinutes(10))
                    .build();
            ReflectionTestUtils.setField(existingLog, "id", 500L);

            when(userMissionLogRepository.findByUserIdAndTargetDate(user.getId(), today))
                    .thenReturn(Optional.of(existingLog));

            LocalDateTime now = LocalDateTime.of(today, detoxStart.plusMinutes(1));

            // when
            MissionTodayResponse response = missionService.getOrCreateTodayMission(deviceId, today, now);

            // then
            assertThat(response.missionLogId()).isEqualTo(500L);
            assertThat(response.missionId()).isEqualTo(100L);
            assertThat(response.title()).isEqualTo("물 한 잔 마시기");
            assertThat(response.status()).isEqualTo(MissionStatus.PENDING);
            assertThat(response.popupRequired()).isTrue(); // popupShownAt 이 null 이므로 true
            verify(userMissionLogRepository, never()).save(any(UserMissionLog.class));
        }

        @Test
        @DisplayName("오늘 배정된 미션 로그가 없을 경우 신규 미션 로그를 배정하여 생성하고 반환한다.")
        void createNewMissionLog() {
            // given
            when(userRepository.findByDeviceId(deviceId)).thenReturn(Optional.of(user));
            when(userMissionLogRepository.findByUserIdAndTargetDate(user.getId(), today))
                    .thenReturn(Optional.empty());
            when(missionRepository.findFirstByOrderByIdAsc()).thenReturn(Optional.of(mission));

            LocalDateTime assignedAt = LocalDateTime.of(today, detoxStart);
            LocalDateTime deadlineAt = assignedAt.plusMinutes(10);

            UserMissionLog savedLog = UserMissionLog.builder()
                    .user(user)
                    .mission(mission)
                    .targetDate(today)
                    .status(MissionStatus.PENDING)
                    .assignedAt(assignedAt)
                    .deadlineAt(deadlineAt)
                    .build();
            ReflectionTestUtils.setField(savedLog, "id", 777L);

            when(userMissionLogRepository.save(any(UserMissionLog.class))).thenReturn(savedLog);

            LocalDateTime now = LocalDateTime.of(today, detoxStart.plusMinutes(1));

            // when
            MissionTodayResponse response = missionService.getOrCreateTodayMission(deviceId, today, now);

            // then
            assertThat(response.missionLogId()).isEqualTo(777L);
            assertThat(response.missionId()).isEqualTo(100L);
            assertThat(response.title()).isEqualTo("물 한 잔 마시기");
            assertThat(response.status()).isEqualTo(MissionStatus.PENDING);
            assertThat(response.assignedAt()).isEqualTo(assignedAt);
            assertThat(response.deadlineAt()).isEqualTo(deadlineAt);
            assertThat(response.popupRequired()).isTrue();
            verify(userMissionLogRepository, times(1)).save(any(UserMissionLog.class));
        }

        @Test
        @DisplayName("신규 미션을 배정하려는데 DB에 미션 데이터가 전혀 없을 경우 500 예외를 발생시킨다.")
        void noMissionDataInDb() {
            // given
            when(userRepository.findByDeviceId(deviceId)).thenReturn(Optional.of(user));
            when(userMissionLogRepository.findByUserIdAndTargetDate(user.getId(), today))
                    .thenReturn(Optional.empty());
            when(missionRepository.findFirstByOrderByIdAsc()).thenReturn(Optional.empty());

            LocalDateTime now = LocalDateTime.of(today, detoxStart.plusMinutes(1));

            // when & then
            assertThatThrownBy(() -> missionService.getOrCreateTodayMission(deviceId, today, now))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining(ErrorCode.MISSION_ERROR_500_NO_MISSION_DATA.getMessage());
        }
    }
}
