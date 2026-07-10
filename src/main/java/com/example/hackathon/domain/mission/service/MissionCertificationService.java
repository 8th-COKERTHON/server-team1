package com.example.hackathon.domain.mission.service;

import com.example.hackathon.domain.image.service.StorageService;
import com.example.hackathon.domain.mission.dto.response.MissionCertificationResponse;
import com.example.hackathon.domain.mission.dto.response.MissionCertificationRetakeResponse;
import com.example.hackathon.domain.mission.entity.MissionStatus;
import com.example.hackathon.domain.mission.entity.UserMissionLog;
import com.example.hackathon.domain.mission.repository.UserMissionLogRepository;
import com.example.hackathon.domain.user.entity.User;
import com.example.hackathon.domain.user.repository.UserRepository;
import com.example.hackathon.global.exception.BusinessException;
import com.example.hackathon.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class MissionCertificationService {

    private final UserRepository userRepository;
    private final UserMissionLogRepository userMissionLogRepository;
    private final StorageService storageService;
    private final Clock clock;

    @Transactional(noRollbackFor = BusinessException.class)
    public MissionCertificationResponse certify(String deviceId, MultipartFile image) {
        LocalDateTime now = LocalDateTime.now(clock);
        User user = findUser(deviceId);
        UserMissionLog log = findTodayLogForUpdate(user, now);
        validateInitialCertification(log, now);

        String newImageUrl = storageService.uploadMissionImage(log.getId(), image);
        deleteOnRollback(newImageUrl, log.getId());
        try {
            log.certify(newImageUrl, now);
            userMissionLogRepository.saveAndFlush(log);
        } catch (RuntimeException exception) {
            deleteQuietly(newImageUrl, log.getId());
            throw exception;
        }

        LocalDateTime detoxEnd = resolveDetoxEndDateTime(
                log.getTargetDate(), user.getDetoxStartTime(), user.getDetoxEndTime());
        return MissionCertificationResponse.of(log, user.getDetoxEndTime(), now.isBefore(detoxEnd));
    }

    @Transactional
    public MissionCertificationRetakeResponse retake(String deviceId, MultipartFile image) {
        LocalDateTime now = LocalDateTime.now(clock);
        User user = findUser(deviceId);
        UserMissionLog log = findTodayLogForUpdate(user, now);
        LocalDateTime detoxEnd = resolveDetoxEndDateTime(
                log.getTargetDate(), user.getDetoxStartTime(), user.getDetoxEndTime());
        validateRetake(log, now, detoxEnd);

        String oldImageUrl = log.getImageUrl();
        String newImageUrl = storageService.uploadMissionImage(log.getId(), image);
        deleteOnRollback(newImageUrl, log.getId());
        try {
            log.replaceCertificationImage(newImageUrl);
            userMissionLogRepository.saveAndFlush(log);
        } catch (RuntimeException exception) {
            deleteQuietly(newImageUrl, log.getId());
            throw exception;
        }
        deleteAfterCommit(oldImageUrl, log.getId());

        return MissionCertificationRetakeResponse.of(
                log, now, user.getDetoxEndTime(), now.isBefore(detoxEnd));
    }

    private User findUser(String deviceId) {
        User user = userRepository.findByDeviceId(deviceId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_ERROR_404_NOT_FOUND));
        if (user.getDetoxStartTime() == null || user.getDetoxEndTime() == null) {
            throw new BusinessException(ErrorCode.MISSION_ERROR_400_DETOX_TIME_NOT_SET);
        }
        return user;
    }

    private UserMissionLog findTodayLogForUpdate(User user, LocalDateTime now) {
        LocalDate targetDate = resolveTargetDate(user, now);
        return userMissionLogRepository.findByUserIdAndTargetDateForUpdate(user.getId(), targetDate)
                .orElseThrow(() -> new BusinessException(ErrorCode.MISSION_ERROR_404_NOT_FOUND));
    }

    private void validateInitialCertification(UserMissionLog log, LocalDateTime now) {
        if (now.isBefore(log.getAssignedAt())) {
            throw new BusinessException(ErrorCode.MISSION_ERROR_400_BEFORE_DETOX_START);
        }
        if (now.isAfter(log.getDeadlineAt())
                && (log.getStatus() == MissionStatus.ASSIGNED || log.getStatus() == MissionStatus.CONFIRMED)) {
            log.updateStatus(MissionStatus.FAILED);
            userMissionLogRepository.saveAndFlush(log);
            throw new BusinessException(ErrorCode.MISSION_ERROR_400_DEADLINE_EXCEEDED);
        }
        if (log.getStatus() == MissionStatus.FAILED) {
            throw new BusinessException(ErrorCode.MISSION_ERROR_400_ALREADY_FAILED);
        }
        if (log.getStatus() == MissionStatus.SUCCESS) {
            throw new BusinessException(ErrorCode.MISSION_ERROR_400_ALREADY_CERTIFIED);
        }
    }

    private void validateRetake(UserMissionLog log, LocalDateTime now, LocalDateTime detoxEnd) {
        if (log.getStatus() != MissionStatus.SUCCESS) {
            throw new BusinessException(ErrorCode.MISSION_ERROR_400_CERTIFICATION_NOT_FOUND);
        }
        if (log.getImageUrl() == null || log.getImageUrl().isBlank()) {
            throw new BusinessException(ErrorCode.MISSION_ERROR_400_CERTIFICATION_NOT_FOUND);
        }
        if (!now.isBefore(detoxEnd)) {
            throw new BusinessException(ErrorCode.MISSION_ERROR_400_DETOX_ALREADY_ENDED);
        }
    }

    private LocalDate resolveTargetDate(User user, LocalDateTime now) {
        LocalTime startTime = user.getDetoxStartTime();
        LocalTime endTime = user.getDetoxEndTime();
        if (!endTime.isAfter(startTime) && now.toLocalTime().isBefore(endTime)) {
            return now.toLocalDate().minusDays(1);
        }
        return now.toLocalDate();
    }

    private LocalDateTime resolveDetoxEndDateTime(
            LocalDate targetDate,
            LocalTime startTime,
            LocalTime endTime
    ) {
        LocalDate endDate = endTime.isAfter(startTime) ? targetDate : targetDate.plusDays(1);
        return LocalDateTime.of(endDate, endTime);
    }

    private void deleteAfterCommit(String imageUrl, Long missionLogId) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            deleteQuietly(imageUrl, missionLogId);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                deleteQuietly(imageUrl, missionLogId);
            }
        });
    }

    private void deleteOnRollback(String imageUrl, Long missionLogId) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status != TransactionSynchronization.STATUS_COMMITTED) {
                    deleteQuietly(imageUrl, missionLogId);
                }
            }
        });
    }

    private void deleteQuietly(String imageUrl, Long missionLogId) {
        try {
            storageService.delete(imageUrl);
        } catch (RuntimeException exception) {
            log.warn("Failed to delete mission image; missionLogId={}, imageUrl={}",
                    missionLogId, imageUrl, exception);
        }
    }
}
