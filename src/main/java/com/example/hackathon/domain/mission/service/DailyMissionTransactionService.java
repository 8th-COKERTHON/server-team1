package com.example.hackathon.domain.mission.service;

import com.example.hackathon.domain.mission.entity.DailyMission;
import com.example.hackathon.domain.mission.entity.UserMissionLog;
import com.example.hackathon.domain.mission.repository.DailyMissionRepository;
import com.example.hackathon.domain.mission.repository.UserMissionLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DailyMissionTransactionService {

    private final DailyMissionRepository dailyMissionRepository;
    private final UserMissionLogRepository userMissionLogRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public DailyMission saveDailyMission(DailyMission dailyMission) {
        return dailyMissionRepository.saveAndFlush(dailyMission);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public UserMissionLog saveUserMissionLog(UserMissionLog userMissionLog) {
        return userMissionLogRepository.saveAndFlush(userMissionLog);
    }
}
