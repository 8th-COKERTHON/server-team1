package com.example.hackathon.domain.mission.service;

import com.example.hackathon.domain.mission.entity.MissionStatus;
import com.example.hackathon.domain.mission.entity.UserMissionLog;
import com.example.hackathon.domain.mission.repository.UserMissionLogRepository;
import com.example.hackathon.global.exception.BusinessException;
import com.example.hackathon.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class MissionCertificationTransactionService {

    private final UserMissionLogRepository userMissionLogRepository;

    @Transactional(noRollbackFor = BusinessException.class)
    public UserMissionLog certifyUploadedImage(
            Long userId,
            LocalDate targetDate,
            String imageUrl,
            LocalDateTime now
    ) {
        UserMissionLog log = userMissionLogRepository
                .findByUserIdAndTargetDateForUpdate(userId, targetDate)
                .orElseThrow(() -> new BusinessException(ErrorCode.MISSION_ERROR_404_NOT_FOUND));
        validateInitialCertification(log, now);
        log.certify(imageUrl, now);
        return userMissionLogRepository.saveAndFlush(log);
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
}
