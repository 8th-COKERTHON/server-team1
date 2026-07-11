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
    @JoinColumn(name = "daily_mission_id", nullable = false)
    private DailyMission dailyMission;

    @Column(name = "team_id")
    private Long teamId;

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

    @Column(name = "retry_count", nullable = false)
    private int retryCount = 0;

    @Column(name = "image_url", length = 512)
    private String imageUrl;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Builder
    private UserMissionLog(User user, DailyMission dailyMission, Long teamId, Integer retryCount,
                           LocalDate targetDate, MissionStatus status,
                           LocalDateTime assignedAt, LocalDateTime deadlineAt) {
        this.user = user;
        this.dailyMission = dailyMission;
        this.teamId = teamId;
        this.retryCount = retryCount != null ? retryCount : 0;
        this.targetDate = targetDate;
        this.status = status;
        this.assignedAt = assignedAt;
        this.deadlineAt = deadlineAt;
    }

    public void showPopup(LocalDateTime time) {
        if (this.popupShownAt == null) {
            this.popupShownAt = time;
        }
    }

    public void updateStatus(MissionStatus targetStatus) {
        validateStatusTransition(this.status, targetStatus);
        this.status = targetStatus;
    }

    public void certify(String imageUrl, LocalDateTime completedAt) {
        updateStatus(MissionStatus.SUCCESS);
        this.imageUrl = imageUrl;
        if (this.completedAt == null) {
            this.completedAt = completedAt;
        }
    }

    public void replaceCertificationImage(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    private void validateStatusTransition(MissionStatus current, MissionStatus target) {
        if (current == target) {
            return;
        }
        if (current == MissionStatus.FAILED) {
            throw new com.example.hackathon.global.exception.BusinessException(
                com.example.hackathon.global.exception.ErrorCode.MISSION_ERROR_400_ALREADY_FAILED
            );
        }
        if (current == MissionStatus.SUCCESS) {
            throw new com.example.hackathon.global.exception.BusinessException(
                com.example.hackathon.global.exception.ErrorCode.MISSION_ERROR_400_ALREADY_COMPLETED
            );
        }
        
        if (current == MissionStatus.ASSIGNED) {
            if (target == MissionStatus.CONFIRMED || target == MissionStatus.FAILED || target == MissionStatus.SUCCESS) {
                return;
            }
        }
        if (current == MissionStatus.CONFIRMED) {
            if (target == MissionStatus.FAILED || target == MissionStatus.SUCCESS) {
                return;
            }
        }
        
        throw new com.example.hackathon.global.exception.BusinessException(
            com.example.hackathon.global.exception.ErrorCode.MISSION_ERROR_400_INVALID_TRANSITION
        );
    }

    public boolean isPopupRequired(LocalDateTime now) {
        return !now.isBefore(this.assignedAt)
                && now.isBefore(this.deadlineAt)
                && (this.status == MissionStatus.ASSIGNED || this.status == MissionStatus.CONFIRMED);
    }
}
