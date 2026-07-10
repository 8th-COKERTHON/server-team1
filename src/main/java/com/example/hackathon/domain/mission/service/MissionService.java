package com.example.hackathon.domain.mission.service;

import com.example.hackathon.domain.mission.dto.response.MissionTodayResponse;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MissionService {

    private final UserRepository userRepository;
    private final MissionRepository missionRepository;
    private final DailyMissionRepository dailyMissionRepository;
    private final UserMissionLogRepository userMissionLogRepository;

    @Transactional
    public MissionTodayResponse getOrCreateTodayMission(String deviceId, LocalDate todayDate, LocalDateTime nowTime) {
        // 1. 사용자 조회
        User user = userRepository.findByDeviceId(deviceId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_ERROR_404_NOT_FOUND));

        // 2. 디톡스 설정 시간 확인
        if (user.getDetoxStartTime() == null || user.getDetoxEndTime() == null) {
            throw new BusinessException(ErrorCode.MISSION_ERROR_400_DETOX_TIME_NOT_SET);
        }

        // 3. 디톡스 시작 시각 이전인지 확인
        LocalDateTime detoxStartDateTime = LocalDateTime.of(todayDate, user.getDetoxStartTime());
        if (nowTime.isBefore(detoxStartDateTime)) {
            throw new BusinessException(ErrorCode.MISSION_ERROR_400_BEFORE_DETOX_START);
        }

        // 4. 오늘 날짜의 DailyMission 조회 및 생성 (없을 시 생성)
        DailyMission dailyMission = getOrCreateDailyMission(todayDate);

        // 5. 오늘 날짜의 USER_MISSION_LOG 조회 및 생성
        return userMissionLogRepository.findByUserIdAndTargetDate(user.getId(), todayDate)
                .map(MissionTodayResponse::from)
                .orElseGet(() -> {
                    LocalDateTime deadlineDateTime = detoxStartDateTime.plusMinutes(10);

                    UserMissionLog newLog = UserMissionLog.builder()
                            .user(user)
                            .dailyMission(dailyMission)
                            .targetDate(todayDate)
                            .status(MissionStatus.ASSIGNED)
                            .assignedAt(detoxStartDateTime)
                            .deadlineAt(deadlineDateTime)
                            .build();

                    try {
                        UserMissionLog savedLog = userMissionLogRepository.save(newLog);
                        return MissionTodayResponse.from(savedLog);
                    } catch (DataIntegrityViolationException e) {
                        return userMissionLogRepository.findByUserIdAndTargetDate(user.getId(), todayDate)
                                .map(MissionTodayResponse::from)
                                .orElseThrow(() -> e);
                    }
                });
    }

    private DailyMission getOrCreateDailyMission(LocalDate todayDate) {
        return dailyMissionRepository.findByTargetDate(todayDate)
                .orElseGet(() -> {
                    Mission mission = missionRepository.findFirstByIsActiveTrueOrderByIdAsc()
                            .orElseThrow(() -> new BusinessException(ErrorCode.MISSION_ERROR_500_NO_MISSION_DATA));

                    DailyMission newDaily = DailyMission.builder()
                            .mission(mission)
                            .targetDate(todayDate)
                            .build();

                    try {
                        return dailyMissionRepository.save(newDaily);
                    } catch (DataIntegrityViolationException e) {
                        return dailyMissionRepository.findByTargetDate(todayDate)
                                .orElseThrow(() -> e);
                    }
                });
    }
}
