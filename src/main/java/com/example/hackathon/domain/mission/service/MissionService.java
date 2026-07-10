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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
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
    public MissionTodayResponse getOrCreateTodayMission(String deviceId, LocalDate inputDate, LocalDateTime nowTime) {
        // 1. 사용자 조회
        User user = userRepository.findByDeviceId(deviceId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_ERROR_404_NOT_FOUND));

        // 2. 디톡스 설정 시간 확인
        if (user.getDetoxStartTime() == null || user.getDetoxEndTime() == null) {
            throw new BusinessException(ErrorCode.MISSION_ERROR_400_DETOX_TIME_NOT_SET);
        }

        // 자정 넘김 고려한 targetDate 계산
        LocalDate targetDate = calculateTargetDate(user, nowTime);

        // 3. 디톡스 시작 시각 이전인지 확인
        LocalDateTime detoxStartDateTime = LocalDateTime.of(targetDate, user.getDetoxStartTime());
        if (nowTime.isBefore(detoxStartDateTime)) {
            throw new BusinessException(ErrorCode.MISSION_ERROR_400_BEFORE_DETOX_START);
        }

        // 4. 오늘 날짜의 DailyMission 조회 및 생성 (없을 시 생성)
        DailyMission dailyMission = getOrCreateDailyMission(targetDate);

        // 5. 오늘 날짜의 USER_MISSION_LOG 조회 및 생성
        UserMissionLog log = userMissionLogRepository.findByUserIdAndTargetDate(user.getId(), targetDate)
                .orElseGet(() -> {
                    LocalDateTime deadlineDateTime = detoxStartDateTime.plusMinutes(10);

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

        // Lazy 실패 만료 처리 적용
        log = checkAndExpireLog(log, nowTime);

        return MissionTodayResponse.from(log);
    }

    @Transactional
    public MissionTodayStatusResponse getTodayMissionStatus(String deviceId) {
        LocalDateTime now = LocalDateTime.now(clock);
        User user = userRepository.findByDeviceId(deviceId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_ERROR_404_NOT_FOUND));

        if (user.getDetoxStartTime() == null || user.getDetoxEndTime() == null) {
            throw new BusinessException(ErrorCode.MISSION_ERROR_400_DETOX_TIME_NOT_SET);
        }

        LocalDate targetDate = calculateTargetDate(user, now);

        UserMissionLog log = userMissionLogRepository.findByUserIdAndTargetDate(user.getId(), targetDate)
                .orElseGet(() -> {
                    MissionTodayResponse todayResponse = getOrCreateTodayMission(deviceId, targetDate, now);
                    return userMissionLogRepository.findById(todayResponse.missionLogId())
                            .orElseThrow(() -> new BusinessException(ErrorCode.MISSION_ERROR_404_NOT_FOUND));
                });

        // Lazy 만료 처리
        log = checkAndExpireLog(log, now);

        boolean expired = now.isAfter(log.getDeadlineAt());
        long remainingSeconds = Math.max(java.time.Duration.between(now, log.getDeadlineAt()).getSeconds(), 0);

        // popupRequired 판단 조건
        boolean popupRequired = !now.isBefore(log.getAssignedAt()) 
                && now.isBefore(log.getDeadlineAt()) 
                && (log.getStatus() == MissionStatus.ASSIGNED || log.getStatus() == MissionStatus.CONFIRMED);

        return MissionTodayStatusResponse.of(log, popupRequired, remainingSeconds, expired);
    }

    @Transactional
    public MissionPopupResponse recordPopupShown(String deviceId) {
        LocalDateTime now = LocalDateTime.now(clock);
        User user = userRepository.findByDeviceId(deviceId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_ERROR_404_NOT_FOUND));

        if (user.getDetoxStartTime() == null || user.getDetoxEndTime() == null) {
            throw new BusinessException(ErrorCode.MISSION_ERROR_400_DETOX_TIME_NOT_SET);
        }

        LocalDate targetDate = calculateTargetDate(user, now);

        UserMissionLog log = userMissionLogRepository.findByUserIdAndTargetDate(user.getId(), targetDate)
                .orElseGet(() -> {
                    MissionTodayResponse todayResponse = getOrCreateTodayMission(deviceId, targetDate, now);
                    return userMissionLogRepository.findById(todayResponse.missionLogId())
                            .orElseThrow(() -> new BusinessException(ErrorCode.MISSION_ERROR_404_NOT_FOUND));
                });

        // Lazy 만료 처리
        log = checkAndExpireLog(log, now);

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

        long remainingSeconds = Math.max(java.time.Duration.between(now, log.getDeadlineAt()).getSeconds(), 0);
        boolean popupRequired = !now.isBefore(log.getAssignedAt()) 
                && now.isBefore(log.getDeadlineAt()) 
                && (log.getStatus() == MissionStatus.ASSIGNED || log.getStatus() == MissionStatus.CONFIRMED);

        return MissionPopupResponse.of(log, remainingSeconds, popupRequired);
    }

    @Transactional
    public MissionConfirmResponse confirmMission(String deviceId) {
        LocalDateTime now = LocalDateTime.now(clock);
        User user = userRepository.findByDeviceId(deviceId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_ERROR_404_NOT_FOUND));

        if (user.getDetoxStartTime() == null || user.getDetoxEndTime() == null) {
            throw new BusinessException(ErrorCode.MISSION_ERROR_400_DETOX_TIME_NOT_SET);
        }

        LocalDate targetDate = calculateTargetDate(user, now);

        UserMissionLog log = userMissionLogRepository.findByUserIdAndTargetDate(user.getId(), targetDate)
                .orElseGet(() -> {
                    MissionTodayResponse todayResponse = getOrCreateTodayMission(deviceId, targetDate, now);
                    return userMissionLogRepository.findById(todayResponse.missionLogId())
                            .orElseThrow(() -> new BusinessException(ErrorCode.MISSION_ERROR_404_NOT_FOUND));
                });

        // Lazy 만료 처리
        log = checkAndExpireLog(log, now);

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

        long remainingSeconds = Math.max(java.time.Duration.between(now, log.getDeadlineAt()).getSeconds(), 0);

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

    private LocalDate calculateTargetDate(User user, LocalDateTime now) {
        LocalTime startTime = user.getDetoxStartTime();
        LocalDate today = now.toLocalDate();

        LocalDateTime yesterdayStart = LocalDateTime.of(today.minusDays(1), startTime);
        LocalDateTime yesterdayDeadline = yesterdayStart.plusMinutes(10);

        if (now.isBefore(yesterdayDeadline)) {
            return today.minusDays(1);
        }
        return today;
    }

    private UserMissionLog checkAndExpireLog(UserMissionLog log, LocalDateTime now) {
        if (now.isAfter(log.getDeadlineAt()) && 
            (log.getStatus() == MissionStatus.ASSIGNED || log.getStatus() == MissionStatus.CONFIRMED)) {
            log.updateStatus(MissionStatus.FAILED);
            userMissionLogRepository.save(log);
        }
        return log;
    }
}
