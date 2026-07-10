package com.example.hackathon.domain.detox.service;

import com.example.hackathon.domain.detox.dto.DetoxProgressResponse;
import com.example.hackathon.domain.mission.entity.MissionStatus;
import com.example.hackathon.domain.mission.entity.UserMissionLog;
import com.example.hackathon.domain.mission.repository.UserMissionLogRepository;
import com.example.hackathon.domain.team.repository.TeamMemberRepository;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DetoxProgressServiceTest {

    private static final String DEVICE_ID = "detox-progress-device";
    private static final LocalDate TARGET_DATE = LocalDate.of(2026, 7, 11);

    @Mock UserRepository userRepository;
    @Mock UserMissionLogRepository userMissionLogRepository;
    @Mock TeamMemberRepository teamMemberRepository;
    @Mock Clock clock;
    @InjectMocks DetoxProgressService service;

    private User user;
    private UserMissionLog missionLog;

    @BeforeEach
    void setUp() {
        user = user(1L, "사용자", LocalTime.of(22, 0), LocalTime.of(23, 0));
        missionLog = missionLog(MissionStatus.SUCCESS, TARGET_DATE);
    }

    @Test
    void successMissionReturnsProgressAtStartBoundary() {
        prepare(TARGET_DATE.atTime(22, 0), TARGET_DATE, missionLog);
        when(teamMemberRepository.findDistinctTeammatesByUserId(user.getId())).thenReturn(List.of());

        DetoxProgressResponse response = service.getProgress(DEVICE_ID);

        assertThat(response.inProgress()).isTrue();
        assertThat(response.remainingSeconds()).isEqualTo(3600);
        assertThat(response.unlockMessage()).isEqualTo("23:00에 해제됩니다.");
        assertThat(response.overlappingMembers()).isEmpty();
    }

    @ParameterizedTest
    @EnumSource(value = MissionStatus.class, names = {"ASSIGNED", "CONFIRMED", "FAILED"})
    void nonSuccessMissionIsRejected(MissionStatus status) {
        ReflectionTestUtils.setField(missionLog, "status", status);
        prepare(TARGET_DATE.atTime(22, 10), TARGET_DATE, missionLog);

        assertThatThrownBy(() -> service.getProgress(DEVICE_ID))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.MISSION_ERROR_400_NOT_CERTIFIED);
    }

    @Test
    void missingMissionLogIsRejected() {
        setClock(TARGET_DATE.atTime(22, 10));
        when(userRepository.findByDeviceId(DEVICE_ID)).thenReturn(Optional.of(user));
        when(userMissionLogRepository.findByUserIdAndTargetDate(user.getId(), TARGET_DATE))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getProgress(DEVICE_ID))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.MISSION_ERROR_404_NOT_FOUND);
    }

    @Test
    void beforeStartAndAtEndAreNotInProgress() {
        prepare(TARGET_DATE.atTime(21, 59), TARGET_DATE, missionLog);
        DetoxProgressResponse beforeStart = service.getProgress(DEVICE_ID);
        assertThat(beforeStart.inProgress()).isFalse();
        assertThat(beforeStart.remainingSeconds()).isZero();

        reset(clock, userRepository, userMissionLogRepository);
        prepare(TARGET_DATE.atTime(23, 0), TARGET_DATE, missionLog);
        DetoxProgressResponse atEnd = service.getProgress(DEVICE_ID);
        assertThat(atEnd.inProgress()).isFalse();
        assertThat(atEnd.remainingSeconds()).isZero();
        verifyNoInteractions(teamMemberRepository);
    }

    @Test
    void overlappingMembersAreFilteredDeduplicatedAndSorted() {
        prepare(TARGET_DATE.atTime(22, 15), TARGET_DATE, missionLog);
        User laterName = user(2L, "하늘", LocalTime.of(22, 30), LocalTime.of(23, 30));
        User firstName = user(3L, "가람", LocalTime.of(21, 30), LocalTime.of(22, 30));
        User touchingBoundary = user(4L, "경계", LocalTime.of(23, 0), LocalTime.of(23, 30));
        when(teamMemberRepository.findDistinctTeammatesByUserId(user.getId()))
                .thenReturn(List.of(laterName, firstName, laterName, touchingBoundary, user));

        DetoxProgressResponse response = service.getProgress(DEVICE_ID);

        assertThat(response.overlappingMemberCount()).isEqualTo(2);
        assertThat(response.overlappingMembers())
                .extracting(member -> member.nickname())
                .containsExactly("가람", "하늘");
        assertThat(response.overlappingMembers().get(0).overlapStartDateTime())
                .isEqualTo(TARGET_DATE.atTime(22, 0));
        assertThat(response.overlappingMembers().get(0).overlapEndDateTime())
                .isEqualTo(TARGET_DATE.atTime(22, 30));
    }

    @Test
    void allJoinedTeamsAreQueriedThroughSingleRepositoryMethod() {
        prepare(TARGET_DATE.atTime(22, 15), TARGET_DATE, missionLog);
        when(teamMemberRepository.findDistinctTeammatesByUserId(user.getId())).thenReturn(List.of());

        service.getProgress(DEVICE_ID);

        verify(teamMemberRepository, times(1)).findDistinctTeammatesByUserId(user.getId());
        verifyNoMoreInteractions(teamMemberRepository);
    }

    @Test
    void overnightPeriodUsesNextDayAsEnd() {
        user = user(1L, "야간", LocalTime.of(23, 0), LocalTime.of(1, 0));
        missionLog = missionLog(MissionStatus.SUCCESS, TARGET_DATE);
        LocalDateTime now = TARGET_DATE.plusDays(1).atTime(0, 30);
        setClock(now);
        when(userRepository.findByDeviceId(DEVICE_ID)).thenReturn(Optional.of(user));
        when(userMissionLogRepository.findByUserIdAndTargetDate(user.getId(), now.toLocalDate()))
                .thenReturn(Optional.empty());
        when(userMissionLogRepository.findByUserIdAndTargetDate(user.getId(), TARGET_DATE))
                .thenReturn(Optional.of(missionLog));
        User sameTargetDateEarlyMorning = user(2L, "새벽", LocalTime.of(0, 30), LocalTime.of(2, 0));
        User overlappingNight = user(3L, "심야", LocalTime.of(23, 30), LocalTime.of(0, 45));
        when(teamMemberRepository.findDistinctTeammatesByUserId(user.getId()))
                .thenReturn(List.of(sameTargetDateEarlyMorning, overlappingNight));

        DetoxProgressResponse response = service.getProgress(DEVICE_ID);

        assertThat(response.inProgress()).isTrue();
        assertThat(response.endDateTime()).isEqualTo(TARGET_DATE.plusDays(1).atTime(1, 0));
        assertThat(response.remainingSeconds()).isEqualTo(1800);
        assertThat(response.overlappingMembers())
                .extracting(member -> member.nickname())
                .containsExactly("심야");
    }

    @Test
    void teammatesAreEmptyAfterDetoxEnds() {
        prepare(TARGET_DATE.atTime(23, 1), TARGET_DATE, missionLog);

        DetoxProgressResponse response = service.getProgress(DEVICE_ID);

        assertThat(response.inProgress()).isFalse();
        assertThat(response.overlappingMemberCount()).isZero();
        assertThat(response.overlappingMembers()).isEmpty();
        verifyNoInteractions(teamMemberRepository);
    }

    private void prepare(LocalDateTime now, LocalDate targetDate, UserMissionLog log) {
        setClock(now);
        when(userRepository.findByDeviceId(DEVICE_ID)).thenReturn(Optional.of(user));
        when(userMissionLogRepository.findByUserIdAndTargetDate(user.getId(), targetDate))
                .thenReturn(Optional.of(log));
    }

    private void setClock(LocalDateTime now) {
        ZoneId zone = ZoneId.of("Asia/Seoul");
        Instant instant = now.atZone(zone).toInstant();
        when(clock.instant()).thenReturn(instant);
        when(clock.getZone()).thenReturn(zone);
    }

    private User user(Long id, String nickname, LocalTime startTime, LocalTime endTime) {
        User result = User.builder()
                .deviceId("device-" + id)
                .nickname(nickname)
                .detoxStartTime(startTime)
                .detoxEndTime(endTime)
                .build();
        ReflectionTestUtils.setField(result, "id", id);
        return result;
    }

    private UserMissionLog missionLog(MissionStatus status, LocalDate targetDate) {
        UserMissionLog result = UserMissionLog.builder()
                .user(user)
                .targetDate(targetDate)
                .status(status)
                .assignedAt(targetDate.atTime(user.getDetoxStartTime()))
                .deadlineAt(targetDate.atTime(user.getDetoxStartTime()).plusMinutes(10))
                .build();
        ReflectionTestUtils.setField(result, "id", 100L);
        return result;
    }
}
