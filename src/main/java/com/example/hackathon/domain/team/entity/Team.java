package com.example.hackathon.domain.team.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "teams")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Team {

    /** 팀 최대 인원. */
    public static final int MAX_MEMBERS = 4;

    private static final int INITIAL_BRICKS = 0;
    private static final int INITIAL_STAGE = 1;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 10)
    private String name;

    @Column(nullable = false, unique = true, length = 10)
    private String inviteCode;

    @Column(nullable = false)
    private int totalBricks;

    /** 건물 단계. 1단계부터 시작한다. */
    @Column(nullable = false)
    private int stage;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @Builder
    private Team(String name, String inviteCode) {
        this.name = name;
        this.inviteCode = inviteCode;
        this.totalBricks = INITIAL_BRICKS;
        this.stage = INITIAL_STAGE;
    }
}