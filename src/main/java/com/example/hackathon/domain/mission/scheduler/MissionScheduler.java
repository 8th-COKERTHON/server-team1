package com.example.hackathon.domain.mission.scheduler;

import com.example.hackathon.domain.mission.entity.MissionStatus;
import com.example.hackathon.domain.mission.repository.UserMissionLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class MissionScheduler {

    private static final long EXPIRE_MISSIONS_LOCK_ID = 781923451L;

    private final UserMissionLogRepository userMissionLogRepository;
    private final Clock clock;
    private final JdbcTemplate jdbcTemplate;

    @Transactional
    @Scheduled(fixedDelay = 60000)
    public void expireMissions() {
        LocalDateTime now = LocalDateTime.now(clock);
        try {
            Boolean lockAcquired = jdbcTemplate.queryForObject(
                    "select pg_try_advisory_xact_lock(?)",
                    Boolean.class,
                    EXPIRE_MISSIONS_LOCK_ID
            );
            if (!Boolean.TRUE.equals(lockAcquired)) {
                log.debug("Skipping mission expiration because another instance holds the lock; lockId={}",
                        EXPIRE_MISSIONS_LOCK_ID);
                return;
            }

            int updatedCount = userMissionLogRepository.updateExpiredMissions(
                    now,
                    MissionStatus.FAILED,
                    MissionStatus.ASSIGNED,
                    MissionStatus.CONFIRMED
            );
            if (updatedCount > 0) {
                log.info("Expired missions; updatedCount={}, status={}, expiredBefore={}",
                        updatedCount, MissionStatus.FAILED, now);
            }
        } catch (Exception exception) {
            log.error("Mission expiration failed; lockId={}, expiredBefore={}",
                    EXPIRE_MISSIONS_LOCK_ID, now, exception);
        }
    }
}
