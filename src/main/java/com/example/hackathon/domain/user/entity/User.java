package com.example.hackathon.domain.user.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;



@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 255)
    private String deviceId;

    @Column(nullable = false, length = 30)
    private String nickname;

    @Column(name = "detox_start_time")
    private LocalTime detoxStartTime;

    @Column(name = "detox_end_time")
    private LocalTime detoxEndTime;

    @Column(name = "personal_stage")
    private Integer personalStage;

    @Column(name = "personal_bricks", nullable = false)
    private int personalBricks = 0;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;



    @Builder
    private User(String deviceId, String nickname, LocalTime detoxStartTime, LocalTime detoxEndTime) {
        this.deviceId = deviceId;
        this.nickname = nickname;
        this.detoxStartTime = detoxStartTime;
        this.detoxEndTime = detoxEndTime;
    }

    public void updateDetoxTime(LocalTime startTime, LocalTime endTime) {
        this.detoxStartTime = startTime;
        this.detoxEndTime = endTime;
    }
}