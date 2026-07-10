package com.example.hackathon.domain.user.entity;

import com.example.hackathon.domain.team.entity.Team;
import com.example.hackathon.global.exception.BusinessException;
import com.example.hackathon.global.exception.ErrorCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
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

    @Column(nullable = false, length = 5)
    private String nickname;

    @Column(name = "detox_start_time")
    private LocalTime detoxStartTime;

    @Column(name = "detox_end_time")
    private LocalTime detoxEndTime;

    @Column(name = "personal_stage")
    private Integer personalStage;

    @Column(name = "personal_bricks", nullable = false)
    private int personalBricks = 0;

    @Column(name = "email", length = 255)
    private String email;

    @Column(name = "email_notification_enabled", nullable = false)
    private boolean emailNotificationEnabled = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id")
    private Team selectedTeam;

    @Column(name = "last_popup_shown_date")
    private LocalDate lastPopupShownDate;

    @Column(name = "last_settlement_date")
    private LocalDate lastSettlementDate;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;



    @Builder
    private User(String deviceId, String nickname, String email, LocalDate lastSettlementDate, LocalTime detoxStartTime, LocalTime detoxEndTime) {
        validateOnboarding(deviceId, nickname);
        this.deviceId = deviceId;
        this.nickname = nickname;
        this.email = email;
        this.detoxStartTime = detoxStartTime;
        this.detoxEndTime = detoxEndTime;
        this.lastSettlementDate = lastSettlementDate != null ? lastSettlementDate : LocalDate.now(ZoneId.of("Asia/Seoul")).minusDays(2);
    }

    private void validateOnboarding(String deviceId, String nickname) {
        if (deviceId == null || deviceId.isBlank()) {
            throw new BusinessException(ErrorCode.COMMON_ERROR_400_INVALID_INPUT, "디바이스 아이디는 필수값입니다.");
        }
        if (nickname == null || nickname.trim().length() < 2 || nickname.trim().length() > 5) {
            throw new BusinessException(ErrorCode.COMMON_ERROR_400_INVALID_INPUT, "닉네임은 2자 이상 5자 이하여야 합니다.");
        }
        if (!nickname.matches("^[a-zA-Z0-9가-힣]*$")) {
            throw new BusinessException(ErrorCode.COMMON_ERROR_400_INVALID_INPUT, "닉네임에는 특수문자나 공백이 포함될 수 없습니다.");
        }
    }

    public void updateDetoxTime(LocalTime startTime, LocalTime endTime) {
        this.detoxStartTime = startTime;
        this.detoxEndTime = endTime;
    }

    public void selectTeam(Team team) {
        this.selectedTeam = team;
    }

    public void updatePersonalBricks(int delta) {
        this.personalBricks += delta;
        if (this.personalBricks < 0) {
            this.personalBricks = 0;
        }
    }

    public void updateEmail(String email) {
        this.email = email;
    }

    public void toggleEmailNotification(boolean enabled) {
        this.emailNotificationEnabled = enabled;
    }

    public void updateLastPopupShownDate(LocalDate date) {
        this.lastPopupShownDate = date;
    }

    public void updateLastSettlementDate(LocalDate date) {
        this.lastSettlementDate = date;
    }

    public void updateEmailNotificationEnabled(boolean enabled) {
        this.emailNotificationEnabled = enabled;
    }
}