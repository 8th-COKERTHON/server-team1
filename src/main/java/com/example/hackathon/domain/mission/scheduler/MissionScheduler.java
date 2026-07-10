package com.example.hackathon.domain.mission.scheduler;

import com.example.hackathon.domain.mission.repository.UserMissionLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class MissionScheduler {

    private final UserMissionLogRepository userMissionLogRepository;
    private final Clock clock;

    @Transactional
    @Scheduled(fixedDelay = 60000)
    public void expireMissions() {
        LocalDateTime now = LocalDateTime.now(clock);
        int updatedCount = userMissionLogRepository.updateExpiredMissions(now);
        if (updatedCount > 0) {
            log.info("Expired {} missions to FAILED status at {}", updatedCount, now);
        }
    }
}
