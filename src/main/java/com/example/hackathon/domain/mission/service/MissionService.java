package com.example.hackathon.domain.mission.service;

import com.example.hackathon.domain.mission.dto.response.MissionConfirmResponse;
import com.example.hackathon.domain.mission.dto.response.MissionPopupResponse;
import com.example.hackathon.domain.mission.dto.response.MissionTodayResponse;
import com.example.hackathon.domain.mission.dto.response.MissionTodayStatusResponse;
import com.example.hackathon.domain.mission.entity.DailyMission;
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
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MissionService {

    private final UserRepository userRepository;
    private final MissionRepository missionRepository;
    private final DailyMissionRepository dailyMissionRepository;
    private final UserMissionLogRepository userMissionLogRepository;
    private final DailyMissionTransactionService transactionService;
    private final Clock clock;

    @Transactional
    public MissionTodayResponse getOrCreateTodayMission(String deviceId) {
        LocalDateTime now = LocalDateTime.now(clock);
        return MissionTodayResponse.from(getOrCreateTodayMissionLog(deviceId, now), now);
    }

    private UserMissionLog getOrCreateTodayMissionLog(String deviceId, LocalDateTime nowTime) {
        User user = userRepository.findByDeviceId(deviceId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_ERROR_404_NOT_FOUND));
        if (user.getDetoxStartTime() == null || user.getDetoxEndTime() == null) {
            throw new BusinessException(ErrorCode.MISSION_ERROR_400_DETOX_TIME_NOT_SET);
        }
        LocalDate targetDate = MissionTargetDateResolver.resolve(user, nowTime);
        return getOrCreateTodayMissionLog(user, targetDate, nowTime);
    }

    private UserMissionLog getOrCreateTodayMissionLog(
            User user,
            LocalDate targetDate,
            LocalDateTime nowTime
    ) {
        LocalDateTime detoxStartDateTime = LocalDateTime.of(targetDate, user.getDetoxStartTime());
        if (nowTime.isBefore(detoxStartDateTime)) {
            throw new BusinessException(ErrorCode.MISSION_ERROR_400_BEFORE_DETOX_START);
        }

        DailyMission dailyMission = getOrCreateDailyMission(targetDate);
        UserMissionLog log = userMissionLogRepository.findByUserIdAndTargetDate(user.getId(), targetDate)
                .orElseGet(() -> {
                    LocalDateTime detoxEndDateTime = LocalDateTime.of(targetDate, user.getDetoxEndTime());
                    if (!detoxEndDateTime.isAfter(detoxStartDateTime)) {
                        detoxEndDateTime = detoxEndDateTime.plusDays(1); // 자정 넘김 처리
                    }

                    LocalDateTime deadlineDateTime = detoxStartDateTime
                            .plusMinutes(MissionTargetDateResolver.DEADLINE_MINUTES);

                    if (detoxEndDateTime.isBefore(deadlineDateTime)) {
                        deadlineDateTime = detoxEndDateTime; // 1분 등 단기 디톡스 설정 시 디톡스 종료 시각으로 타이머 단축
                    }

                    UserMissionLog newLog = UserMissionLog.builder()
                            .user(user)
                            .dailyMission(dailyMission)
                            .targetDate(targetDate)
                            .status(MissionStatus.ASSIGNED)
                            .assignedAt(detoxStartDateTime)
                            .deadlineAt(deadlineDateTime)
                            .build();

                    return saveOrFetchExisting(
                            () -> transactionService.saveUserMissionLog(newLog),
                            () -> userMissionLogRepository.findByUserIdAndTargetDate(user.getId(), targetDate)
                    );
                });

        return checkAndExpireLog(log, nowTime);
    }

    @Transactional
    public MissionTodayStatusResponse getTodayMissionStatus(String deviceId) {
        LocalDateTime now = LocalDateTime.now(clock);
        UserMissionLog log = resolveTodayLog(deviceId, now);

        boolean expired = now.isAfter(log.getDeadlineAt());
        long remainingSeconds = calculateRemainingSeconds(log, now);

        // popupRequired 판단 조건
        boolean popupRequired = log.isPopupRequired(now);

        return MissionTodayStatusResponse.of(log, popupRequired, remainingSeconds, expired);
    }

    @Transactional
    public MissionPopupResponse recordPopupShown(String deviceId) {
        LocalDateTime now = LocalDateTime.now(clock);
        UserMissionLog log = resolveTodayLog(deviceId, now);

        // 예외 검증
        if (now.isBefore(log.getAssignedAt())) {
            throw new BusinessException(ErrorCode.MISSION_ERROR_400_BEFORE_DETOX_START);
        }
        if (log.getStatus() == MissionStatus.FAILED) {
            throw new BusinessException(ErrorCode.MISSION_ERROR_400_ALREADY_FAILED);
        }
        if (log.getStatus() == MissionStatus.SUCCESS) {
            throw new BusinessException(ErrorCode.MISSION_ERROR_400_ALREADY_COMPLETED);
        }

        // 팝업 최초 노출 처리
        log.showPopup(now);
        userMissionLogRepository.save(log);

        long remainingSeconds = calculateRemainingSeconds(log, now);
        boolean popupRequired = log.isPopupRequired(now);

        return MissionPopupResponse.of(log, remainingSeconds, popupRequired);
    }

    @Transactional
    public MissionConfirmResponse confirmMission(String deviceId) {
        LocalDateTime now = LocalDateTime.now(clock);
        UserMissionLog log = resolveTodayLog(deviceId, now);

        // 예외 검증
        if (now.isBefore(log.getAssignedAt())) {
            throw new BusinessException(ErrorCode.MISSION_ERROR_400_BEFORE_DETOX_START);
        }
        if (log.getStatus() == MissionStatus.FAILED) {
            throw new BusinessException(ErrorCode.MISSION_ERROR_400_ALREADY_FAILED);
        }
        if (log.getStatus() == MissionStatus.SUCCESS) {
            throw new BusinessException(ErrorCode.MISSION_ERROR_400_ALREADY_COMPLETED);
        }

        // 상태 전환
        log.updateStatus(MissionStatus.CONFIRMED);
        userMissionLogRepository.save(log);

        long remainingSeconds = calculateRemainingSeconds(log, now);

        return MissionConfirmResponse.of(log, remainingSeconds);
    }

    private DailyMission getOrCreateDailyMission(LocalDate todayDate) {
        return dailyMissionRepository.findByTargetDate(todayDate)
                .orElseGet(() -> {
                    Mission mission = selectRandomActiveMission(todayDate);

                    DailyMission newDaily = DailyMission.builder()
                            .mission(mission)
                            .targetDate(todayDate)
                            .build();

                    return saveOrFetchExisting(
                            () -> transactionService.saveDailyMission(newDaily),
                            () -> dailyMissionRepository.findByTargetDate(todayDate)
                    );
                });
    }

    private Mission selectRandomActiveMission(LocalDate todayDate) {
        List<Mission> activeMissions = missionRepository.findAllByIsActiveTrue();
        if (activeMissions.isEmpty()) {
            throw new BusinessException(ErrorCode.MISSION_ERROR_404_ACTIVE_NOT_FOUND);
        }

        int m = activeMissions.size();

        // targetDate 이전 최근 M개의 DailyMission을 가져온다.
        List<DailyMission> recentDailyMissions = dailyMissionRepository
                .findByTargetDateBeforeOrderByTargetDateDesc(todayDate, PageRequest.of(0, m));

        Set<Long> activeMissionIds = activeMissions.stream()
                .map(Mission::getId)
                .collect(Collectors.toSet());

        Set<Long> usedMissionIds = recentDailyMissions.stream()
                .map(dm -> dm.getMission().getId())
                .filter(activeMissionIds::contains)
                .collect(Collectors.toSet());

        Long lastMissionId = recentDailyMissions.isEmpty() ? null : recentDailyMissions.get(0).getMission().getId();

        List<Mission> candidates;
        if (usedMissionIds.size() < m) {
            // 현재 순환에서 아직 사용되지 않은 미션들 중 선택
            candidates = activeMissions.stream()
                    .filter(mission -> !usedMissionIds.contains(mission.getId()))
                    .collect(Collectors.toList());
        } else {
            // 모든 활성 미션이 소진되어 새로운 순환 시작
            candidates = new ArrayList<>(activeMissions);
        }

        // 직전 날짜의 미션은 활성 미션이 2개 이상일 때 후보에서 제외
        if (candidates.size() >= 2 && lastMissionId != null) {
            candidates.removeIf(mission -> mission.getId().equals(lastMissionId));
        }

        // 남은 후보 중 랜덤 선택
        int randomIndex = java.util.concurrent.ThreadLocalRandom.current().nextInt(candidates.size());
        return candidates.get(randomIndex);
    }

    private <T> T saveOrFetchExisting(Supplier<T> saveOperation, Supplier<Optional<T>> fetchOperation) {
        try {
            return saveOperation.get();
        } catch (DataIntegrityViolationException e) {
            return fetchOperation.get()
                    .orElseThrow(() -> e);
        }
    }

    private UserMissionLog checkAndExpireLog(UserMissionLog log, LocalDateTime now) {
        if (now.isAfter(log.getDeadlineAt()) && 
            (log.getStatus() == MissionStatus.ASSIGNED || log.getStatus() == MissionStatus.CONFIRMED)) {
            log.updateStatus(MissionStatus.FAILED);
            userMissionLogRepository.save(log);
        }
        return log;
    }

    private UserMissionLog resolveTodayLog(String deviceId, LocalDateTime now) {
        User user = userRepository.findByDeviceId(deviceId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_ERROR_404_NOT_FOUND));

        if (user.getDetoxStartTime() == null || user.getDetoxEndTime() == null) {
            throw new BusinessException(ErrorCode.MISSION_ERROR_400_DETOX_TIME_NOT_SET);
        }

        LocalDate targetDate = MissionTargetDateResolver.resolve(user, now);
        UserMissionLog log = userMissionLogRepository.findByUserIdAndTargetDate(user.getId(), targetDate)
                .orElseGet(() -> getOrCreateTodayMissionLog(user, targetDate, now));

        return checkAndExpireLog(log, now);
    }

    private long calculateRemainingSeconds(UserMissionLog log, LocalDateTime now) {
        return Math.max(Duration.between(now, log.getDeadlineAt()).getSeconds(), 0);
    }
}
