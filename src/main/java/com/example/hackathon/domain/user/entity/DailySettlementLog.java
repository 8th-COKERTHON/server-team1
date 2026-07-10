package com.example.hackathon.domain.user.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(
    name = "daily_settlement_log",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uq_daily_settlement_log_user_target_date",
            columnNames = {"user_id", "target_date"}
        ),
        @UniqueConstraint(
            name = "uq_daily_settlement_log_team_target_date",
            columnNames = {"team_id", "target_date"}
        )
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DailySettlementLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "team_id")
    private Long teamId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "target_date", nullable = false)
    private LocalDate targetDate;

    @Column(name = "success", nullable = false)
    private boolean success;

    @Column(name = "brick_before", nullable = false)
    private int brickBefore;

    @Column(name = "brick_after", nullable = false)
    private int brickAfter;

    @Builder
    private DailySettlementLog(Long teamId, Long userId, LocalDate targetDate,
                               boolean success, int brickBefore, int brickAfter) {
        this.teamId = teamId;
        this.userId = userId;
        this.targetDate = targetDate;
        this.success = success;
        this.brickBefore = brickBefore;
        this.brickAfter = brickAfter;
    }
}
