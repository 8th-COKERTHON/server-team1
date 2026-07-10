package com.example.hackathon.domain.mission.repository;

import com.example.hackathon.domain.mission.entity.DailyMission;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DailyMissionRepository extends JpaRepository<DailyMission, Long> {

    Optional<DailyMission> findByTargetDate(LocalDate targetDate);

    List<DailyMission> findByTargetDateBeforeOrderByTargetDateDesc(LocalDate targetDate, Pageable pageable);
}
