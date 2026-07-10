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

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 10)
    private String name;

    @Column(nullable = false, unique = true, length = 10)
    private String inviteCode;

    @Column(nullable = false)
    private int totalBricks;

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
    }

    public void updateTotalBricks(int delta) {
        this.totalBricks += delta;
        if (this.totalBricks < 0) {
            this.totalBricks = 0;
        }
    }
}