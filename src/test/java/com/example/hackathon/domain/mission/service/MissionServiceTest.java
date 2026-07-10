package com.example.hackathon.domain.mission.service;

import com.example.hackathon.domain.mission.dto.response.MissionTodayResponse;
import com.example.hackathon.domain.mission.entity.DailyMission;
import com.example.hackathon.domain.mission.entity.Difficulty;
import com.example.hackathon.domain.mission.entity.Mission;
import com.example.hackathon.domain.mission.entity.MissionStatus;
import com.example.hackathon.domain.mission.entity.UserMissionLog;
import com.example.hackathon.domain.mission.repository.DailyMissionRepository;
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
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
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
    private DailyMissionRepository dailyMissionRepository;

    @Mock
    private UserMissionLogRepository userMissionLogRepository;

    @Mock
    private DailyMissionTransactionService transactionService;

    private final String deviceId = "test-device-id";
    private final LocalDate today = LocalDate.of(2026, 7, 10);
    private final LocalTime detoxStart = LocalTime.of(22, 0);
    private final LocalTime detoxEnd = LocalTime.of(23, 0);

    private User user;
    private Mission mission;
    private DailyMission dailyMission;

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
    }

    @Nested
    @DisplayName("오늘의 미션 조회 및 생성 테스트")
    class GetOrCreateTodayMissionTest {

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
            when(dailyMissionRepository.findByTargetDate(today)).thenReturn(Optional.of(dailyMission));

            LocalDateTime assignedAt = LocalDateTime.of(today, detoxStart);
            UserMissionLog existingLog = UserMissionLog.builder()
                    .user(user)
                    .dailyMission(dailyMission)
                    .targetDate(today)
                    .status(MissionStatus.ASSIGNED)
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
            assertThat(response.status()).isEqualTo(MissionStatus.ASSIGNED);
            assertThat(response.popupRequired()).isTrue();
            verify(transactionService, never()).saveUserMissionLog(any(UserMissionLog.class));
        }

        @Test
        @DisplayName("오늘 배정된 미션 로그가 없을 경우 신규 미션 로그를 배정하여 생성하고 반환한다 (ArgumentCaptor 검증 적용).")
        void createNewMissionLog() {
            // given
            when(userRepository.findByDeviceId(deviceId)).thenReturn(Optional.of(user));
            when(dailyMissionRepository.findByTargetDate(today)).thenReturn(Optional.of(dailyMission));
            when(userMissionLogRepository.findByUserIdAndTargetDate(user.getId(), today))
                    .thenReturn(Optional.empty());

            LocalDateTime assignedAt = LocalDateTime.of(today, detoxStart);
            LocalDateTime deadlineAt = assignedAt.plusMinutes(10);

            UserMissionLog savedLog = UserMissionLog.builder()
                    .user(user)
                    .dailyMission(dailyMission)
                    .targetDate(today)
                    .status(MissionStatus.ASSIGNED)
                    .assignedAt(assignedAt)
                    .deadlineAt(deadlineAt)
                    .build();
            ReflectionTestUtils.setField(savedLog, "id", 777L);

            // saveUserMissionLog 모킹
            when(transactionService.saveUserMissionLog(any(UserMissionLog.class))).thenReturn(savedLog);

            LocalDateTime now = LocalDateTime.of(today, detoxStart.plusMinutes(1));

            // when
            MissionTodayResponse response = missionService.getOrCreateTodayMission(deviceId, today, now);

            // then
            assertThat(response.missionLogId()).isEqualTo(777L);
            assertThat(response.missionId()).isEqualTo(100L);
            assertThat(response.title()).isEqualTo("물 한 잔 마시기");
            assertThat(response.status()).isEqualTo(MissionStatus.ASSIGNED);

            // ArgumentCaptor 검증 추가
            ArgumentCaptor<UserMissionLog> logCaptor = ArgumentCaptor.forClass(UserMissionLog.class);
            verify(transactionService, times(1)).saveUserMissionLog(logCaptor.capture());

            UserMissionLog capturedLog = logCaptor.getValue();
            assertThat(capturedLog.getDailyMission()).isEqualTo(dailyMission);
            assertThat(capturedLog.getTargetDate()).isEqualTo(today);
            assertThat(capturedLog.getStatus()).isEqualTo(MissionStatus.ASSIGNED);
        }

        @Test
        @DisplayName("오늘 존재하는 DailyMission 이 없는 경우 신규 DailyMission 을 생성하고, 새 로그가 이를 참조하는지 검증한다.")
        void createNewDailyMissionAndUserMissionLog() {
            // given
            when(userRepository.findByDeviceId(deviceId)).thenReturn(Optional.of(user));
            when(userMissionLogRepository.findByUserIdAndTargetDate(user.getId(), today))
                    .thenReturn(Optional.empty());

            // 오늘 배정된 DailyMission 이 없으므로 empty() 반환
            when(dailyMissionRepository.findByTargetDate(today)).thenReturn(Optional.empty());

            // 미션 후보군 조회를 모킹
            when(missionRepository.findAllByIsActiveTrue()).thenReturn(List.of(mission));
            // 최근 배정이력 조회를 모킹 (최근 이력 없음)
            when(dailyMissionRepository.findByTargetDateBeforeOrderByTargetDateDesc(eq(today), any()))
                    .thenReturn(Collections.emptyList());

            // saveDailyMission 모킹
            when(transactionService.saveDailyMission(any(DailyMission.class))).thenReturn(dailyMission);

            LocalDateTime assignedAt = LocalDateTime.of(today, detoxStart);
            LocalDateTime deadlineAt = assignedAt.plusMinutes(10);

            UserMissionLog savedLog = UserMissionLog.builder()
                    .user(user)
                    .dailyMission(dailyMission)
                    .targetDate(today)
                    .status(MissionStatus.ASSIGNED)
                    .assignedAt(assignedAt)
                    .deadlineAt(deadlineAt)
                    .build();
            ReflectionTestUtils.setField(savedLog, "id", 888L);

            when(transactionService.saveUserMissionLog(any(UserMissionLog.class))).thenReturn(savedLog);

            LocalDateTime now = LocalDateTime.of(today, detoxStart.plusMinutes(1));

            // when
            MissionTodayResponse response = missionService.getOrCreateTodayMission(deviceId, today, now);

            // then
            assertThat(response.missionLogId()).isEqualTo(888L);
            assertThat(response.missionId()).isEqualTo(100L); // 신규 매핑된 미션 ID
            assertThat(response.title()).isEqualTo("물 한 잔 마시기");

            // Mockito verify assertions 추가 (findAllByIsActiveTrue가 정확히 1번 호출됨을 검증)
            verify(missionRepository, times(1)).findAllByIsActiveTrue();

            // DailyMissionArgumentCaptor 추가 검증
            ArgumentCaptor<DailyMission> dailyMissionCaptor = ArgumentCaptor.forClass(DailyMission.class);
            verify(transactionService, times(1)).saveDailyMission(dailyMissionCaptor.capture());

            DailyMission capturedDaily = dailyMissionCaptor.getValue();
            assertThat(capturedDaily.getMission()).isEqualTo(mission);
            assertThat(capturedDaily.getTargetDate()).isEqualTo(today);

            // UserMissionLogArgumentCaptor 검증
            ArgumentCaptor<UserMissionLog> logCaptor = ArgumentCaptor.forClass(UserMissionLog.class);
            verify(transactionService, times(1)).saveUserMissionLog(logCaptor.capture());
            assertThat(logCaptor.getValue().getDailyMission()).isEqualTo(dailyMission);
        }

        @Test
        @DisplayName("신규 미션을 배정하려는데 DB에 활성화된 미션 데이터가 전혀 없을 경우 404 예외를 발생시킨다.")
        void noActiveMissionInDb() {
            // given
            when(userRepository.findByDeviceId(deviceId)).thenReturn(Optional.of(user));
            when(dailyMissionRepository.findByTargetDate(today)).thenReturn(Optional.empty());
            when(missionRepository.findAllByIsActiveTrue()).thenReturn(Collections.emptyList());

            LocalDateTime now = LocalDateTime.of(today, detoxStart.plusMinutes(1));

            // when & then
            assertThatThrownBy(() -> missionService.getOrCreateTodayMission(deviceId, today, now))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining(ErrorCode.MISSION_ERROR_404_ACTIVE_NOT_FOUND.getMessage());
        }

        @Test
        @DisplayName("신규 미션 로그를 생성 및 저장할 때 DataIntegrityViolationException이 발생하면 기존 미션 로그를 조회하여 반환한다 (ArgumentCaptor 검증 적용).")
        void handleDataIntegrityViolationException() {
            // given
            when(userRepository.findByDeviceId(deviceId)).thenReturn(Optional.of(user));
            when(dailyMissionRepository.findByTargetDate(today)).thenReturn(Optional.of(dailyMission));

            // transactionService 에서 예외 던지도록 설정
            when(transactionService.saveUserMissionLog(any(UserMissionLog.class)))
                    .thenThrow(new org.springframework.dao.DataIntegrityViolationException("Duplicate key value"));

            LocalDateTime assignedAt = LocalDateTime.of(today, detoxStart);
            UserMissionLog existingLog = UserMissionLog.builder()
                    .user(user)
                    .dailyMission(dailyMission)
                    .targetDate(today)
                    .status(MissionStatus.ASSIGNED)
                    .assignedAt(assignedAt)
                    .deadlineAt(assignedAt.plusMinutes(10))
                    .build();
            ReflectionTestUtils.setField(existingLog, "id", 999L);

            // 첫 번째 호출 시 empty(), 두 번째 호출 시 existingLog 반환
            when(userMissionLogRepository.findByUserIdAndTargetDate(user.getId(), today))
                    .thenReturn(Optional.empty())
                    .thenReturn(Optional.of(existingLog));

            LocalDateTime now = LocalDateTime.of(today, detoxStart.plusMinutes(1));

            // when
            MissionTodayResponse response = missionService.getOrCreateTodayMission(deviceId, today, now);

            // then
            assertThat(response.missionLogId()).isEqualTo(999L);
            assertThat(response.missionId()).isEqualTo(100L);
            assertThat(response.status()).isEqualTo(MissionStatus.ASSIGNED);

            // ArgumentCaptor 검증
            ArgumentCaptor<UserMissionLog> logCaptor = ArgumentCaptor.forClass(UserMissionLog.class);
            verify(transactionService, times(1)).saveUserMissionLog(logCaptor.capture());
            assertThat(logCaptor.getValue().getStatus()).isEqualTo(MissionStatus.ASSIGNED);

            verify(userMissionLogRepository, times(2)).findByUserIdAndTargetDate(user.getId(), today);
        }
    }
}
