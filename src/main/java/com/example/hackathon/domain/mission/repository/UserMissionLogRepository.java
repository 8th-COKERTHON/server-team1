package com.example.hackathon.domain.mission.repository;

import com.example.hackathon.domain.mission.entity.UserMissionLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface UserMissionLogRepository extends JpaRepository<UserMissionLog, Long> {

    Optional<UserMissionLog> findByUserIdAndTargetDate(Long userId, LocalDate targetDate);
}
