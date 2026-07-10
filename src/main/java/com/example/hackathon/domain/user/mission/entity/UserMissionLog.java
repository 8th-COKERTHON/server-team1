package com.example.hackathon.domain.user.mission.entity;

import com.example.hackathon.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserMissionLog {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id") // 누가?
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mission_id") // 무슨 미션을?
    private Mission mission;

    private LocalDate targetDate; // 언제?
    private boolean isCompleted;  // 성공?
}
