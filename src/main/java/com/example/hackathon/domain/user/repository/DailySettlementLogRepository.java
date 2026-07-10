package com.example.hackathon.domain.user.repository;

import com.example.hackathon.domain.user.entity.DailySettlementLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;

public interface DailySettlementLogRepository extends JpaRepository<DailySettlementLog, Long> {

    boolean existsByUserIdAndTargetDate(Long userId, LocalDate targetDate);

    boolean existsByTeamIdAndTargetDate(Long teamId, LocalDate targetDate);
}
