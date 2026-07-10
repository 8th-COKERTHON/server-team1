package com.example.hackathon.domain.mission.repository;

import com.example.hackathon.domain.mission.entity.UserMissionLog;
import com.example.hackathon.domain.mission.entity.MissionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

public interface UserMissionLogRepository extends JpaRepository<UserMissionLog, Long> {

    Optional<UserMissionLog> findByUserIdAndTargetDate(Long userId, LocalDate targetDate);

    @Modifying(clearAutomatically = true)
    @Query("update UserMissionLog u set u.status = :failedStatus, u.updatedAt = :now " +
           "where u.status in (:assignedStatus, :confirmedStatus) " +
           "and u.deadlineAt < :now")
    int updateExpiredMissions(
            @Param("now") LocalDateTime now,
            @Param("failedStatus") MissionStatus failedStatus,
            @Param("assignedStatus") MissionStatus assignedStatus,
            @Param("confirmedStatus") MissionStatus confirmedStatus
    );
}
