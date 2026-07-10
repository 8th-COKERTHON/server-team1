package com.example.hackathon.domain.mission.repository;

import com.example.hackathon.domain.mission.entity.Mission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MissionRepository extends JpaRepository<Mission, Long> {

    Optional<Mission> findFirstByOrderByIdAsc();
}
