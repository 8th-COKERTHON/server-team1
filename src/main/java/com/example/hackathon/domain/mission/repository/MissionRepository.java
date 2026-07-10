package com.example.hackathon.domain.mission.repository;

import com.example.hackathon.domain.mission.entity.Mission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MissionRepository extends JpaRepository<Mission, Long> {

    List<Mission> findAllByIsActiveTrue();

    long countByIsActiveTrue();
}
