package com.example.hackathon.domain.mission.repository;

import com.example.hackathon.domain.mission.entity.DailyMission;
import com.example.hackathon.domain.mission.entity.Difficulty;
import com.example.hackathon.domain.mission.entity.Mission;
import com.example.hackathon.domain.mission.entity.MissionStatus;
import com.example.hackathon.domain.mission.entity.UserMissionLog;
import com.example.hackathon.domain.user.entity.User;
import com.example.hackathon.domain.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "aws.region=ap-northeast-2",
                "aws.s3.bucket=test-bucket",
                "aws.s3.access-key=test-access-key",
                "aws.s3.secret-key=test-secret-key",
                "aws.s3.presigned-url-expiration-seconds=300"
        }
)
@Transactional
class UserMissionLogRepositoryTest {

    @Autowired
    private UserMissionLogRepository userMissionLogRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private MissionRepository missionRepository;
    @Autowired
    private DailyMissionRepository dailyMissionRepository;
    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("SUCCESS 상태는 만료 벌크 업데이트 대상에서 제외된다")
    void updateExpiredMissionsDoesNotTargetSuccessStatus() {
        LocalDate targetDate = LocalDate.of(2026, 7, 10);
        LocalDateTime assignedAt = targetDate.atTime(22, 0);
        LocalDateTime deadlineAt = assignedAt.plusMinutes(10);

        User user = userRepository.save(User.builder()
                .deviceId("repository-success-test")
                .nickname("테스터")
                .detoxStartTime(LocalTime.of(22, 0))
                .detoxEndTime(LocalTime.of(23, 0))
                .build());
        Mission mission = missionRepository.save(Mission.builder()
                .title("SUCCESS 유지 테스트")
                .difficulty(Difficulty.EASY)
                .isActive(true)
                .build());
        DailyMission dailyMission = dailyMissionRepository.save(DailyMission.builder()
                .mission(mission)
                .targetDate(targetDate)
                .build());
        UserMissionLog log = userMissionLogRepository.save(UserMissionLog.builder()
                .user(user)
                .dailyMission(dailyMission)
                .targetDate(targetDate)
                .status(MissionStatus.SUCCESS)
                .assignedAt(assignedAt)
                .deadlineAt(deadlineAt)
                .build());
        entityManager.flush();
        entityManager.clear();

        int updatedCount = userMissionLogRepository.updateExpiredMissions(
                deadlineAt.plusMinutes(1),
                MissionStatus.FAILED,
                MissionStatus.ASSIGNED,
                MissionStatus.CONFIRMED
        );
        entityManager.flush();
        entityManager.clear();

        assertThat(updatedCount).isZero();
        assertThat(userMissionLogRepository.findById(log.getId()))
                .get()
                .extracting(UserMissionLog::getStatus)
                .isEqualTo(MissionStatus.SUCCESS);
    }
}
