package com.example.hackathon.domain.team.entity;

import com.example.hackathon.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 사용자 ↔ 팀 N:M 연결. 한 사람이 여러 팀에 속할 수 있다.
 * User 엔티티는 팀을 모른다(참조 없음) — 팀 소속 정보는 전적으로 이 테이블이 갖는다.
 */
@Entity
@Table(
        name = "team_members",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_team_member_user_team",
                columnNames = {"user_id", "team_id"}
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TeamMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime joinedAt;

    private TeamMember(User user, Team team) {
        this.user = user;
        this.team = team;
    }

    public static TeamMember of(User user, Team team) {
        return new TeamMember(user, team);
    }
}
