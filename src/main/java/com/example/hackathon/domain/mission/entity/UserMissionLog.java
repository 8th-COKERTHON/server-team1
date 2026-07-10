package com.example.hackathon.domain.mission.entity;

import com.example.hackathon.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "user_mission_log",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uq_user_mission_log_user_target_date",
            columnNames = {"user_id", "target_date"}
        )
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserMissionLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mission_id", nullable = false)
    private Mission mission;

    @Column(name = "target_date", nullable = false)
    private LocalDate targetDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private MissionStatus status;

    @Column(name = "assigned_at", nullable = false)
    private LocalDateTime assignedAt;

    @Column(name = "deadline_at", nullable = false)
    private LocalDateTime deadlineAt;

    @Column(name = "popup_shown_at")
    private LocalDateTime popupShownAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "image_url", length = 512)
    private String imageUrl;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Builder
    private UserMissionLog(User user, Mission mission, LocalDate targetDate, MissionStatus status,
                           LocalDateTime assignedAt, LocalDateTime deadlineAt) {
        this.user = user;
        this.mission = mission;
        this.targetDate = targetDate;
        this.status = status;
        this.assignedAt = assignedAt;
        this.deadlineAt = deadlineAt;
    }
}
