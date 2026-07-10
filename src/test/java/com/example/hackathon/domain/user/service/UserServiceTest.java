package com.example.hackathon.domain.user.service;

import com.example.hackathon.domain.mission.entity.Difficulty;
import com.example.hackathon.domain.mission.entity.DailyMission;
import com.example.hackathon.domain.mission.entity.Mission;
import com.example.hackathon.domain.mission.entity.MissionStatus;
import com.example.hackathon.domain.mission.entity.UserMissionLog;
import com.example.hackathon.domain.mission.repository.DailyMissionRepository;
import com.example.hackathon.domain.mission.repository.MissionRepository;
import com.example.hackathon.domain.mission.repository.UserMissionLogRepository;
import com.example.hackathon.domain.team.entity.Team;
import com.example.hackathon.domain.team.entity.TeamMember;
import com.example.hackathon.domain.team.repository.TeamMemberRepository;
import com.example.hackathon.domain.team.repository.TeamRepository;
import com.example.hackathon.domain.user.dto.response.UserHomeResponse;
import com.example.hackathon.domain.user.dto.response.UserResponse;
import com.example.hackathon.domain.user.entity.DailySettlementLog;
import com.example.hackathon.domain.user.entity.User;
import com.example.hackathon.domain.user.repository.DailySettlementLogRepository;
import com.example.hackathon.domain.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class UserServiceTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private TeamMemberRepository teamMemberRepository;

    @Autowired
    private DailySettlementLogRepository dailySettlementLogRepository;

    @Autowired
    private UserMissionLogRepository userMissionLogRepository;

    @Autowired
    private MissionRepository missionRepository;

    @Autowired
    private DailyMissionRepository dailyMissionRepository;

    private User createUser(String nickname, String email) {
        return userRepository.save(User.builder()
                .deviceId(UUID.randomUUID().toString())
                .nickname(nickname)
                .email(email)
                .detoxStartTime(LocalTime.of(9, 0))
                .detoxEndTime(LocalTime.of(18, 0))
                .build());
    }

    private Team createTeam(String name) {
        return teamRepository.save(Team.builder()
                .name(name)
                .inviteCode(UUID.randomUUID().toString().substring(0, 8))
                .build());
    }

    @Test
    @DisplayName("사용자 생성 시 이메일 정보가 올바르게 기록된다")
    void createUserWithEmail() {
        // given
        String email = "test@example.com";
        
        // when
        Long userId = userService.createUser("device-123", "홍길동", email);
        
        // then
        User user = userRepository.findById(userId).orElseThrow();
        assertThat(user.getEmail()).isEqualTo(email);
        assertThat(user.isEmailNotificationEnabled()).isTrue();
    }

    @Test
    @DisplayName("이메일 정보 수정 시 정상 저장된다")
    void updateEmailTest() {
        // given
        User user = createUser("홍길동", "old@example.com");
        
        // when
        userService.updateUserEmail(user.getId(), "new@example.com");
        
        // then
        User updated = userRepository.findById(user.getId()).orElseThrow();
        assertThat(updated.getEmail()).isEqualTo("new@example.com");
    }

    @Test
    @DisplayName("개인 모드일 때 전날 미션 성공 시 벽돌이 증가하고 정산 로그가 저장된다")
    void personalModeSettlementSuccess() {
        // given
        User user = createUser("길동", "test@test.com");
        LocalDate yesterday = LocalDate.now(ZoneId.of("Asia/Seoul")).minusDays(1);

        Mission mission = missionRepository.save(Mission.builder().title("일어나기").difficulty(Difficulty.EASY).build());
        DailyMission dm = dailyMissionRepository.save(DailyMission.builder().mission(mission).targetDate(yesterday).build());
        
        userMissionLogRepository.save(UserMissionLog.builder()
                .user(user)
                .dailyMission(dm)
                .targetDate(yesterday)
                .status(MissionStatus.SUCCESS)
                .assignedAt(LocalDateTime.now().minusDays(1))
                .deadlineAt(LocalDateTime.now().minusDays(1).plusMinutes(10))
                .build());

        // when
        UserHomeResponse homeData = userService.getHomeData(user.getId());

        // then
        assertThat(homeData.mode()).isEqualTo("INDIVIDUAL");
        assertThat(homeData.totalBricks()).isEqualTo(1); // 0 -> 1 증가
        assertThat(homeData.popup().showPopup()).isFalse();

        boolean logExists = dailySettlementLogRepository.existsByUserIdAndTargetDate(user.getId(), yesterday);
        assertThat(logExists).isTrue();
    }

    @Test
    @DisplayName("개인 모드일 때 전날 미션 실패 시 벽돌이 차감되며 실패 안내 팝업이 활성화된다")
    void personalModeSettlementFailed() {
        // given
        User user = createUser("길동", "test@test.com");
        user.updatePersonalBricks(2);
        userRepository.saveAndFlush(user);

        LocalDate yesterday = LocalDate.now(ZoneId.of("Asia/Seoul")).minusDays(1);

        // when
        UserHomeResponse homeData = userService.getHomeData(user.getId());

        // then
        assertThat(homeData.mode()).isEqualTo("INDIVIDUAL");
        assertThat(homeData.totalBricks()).isEqualTo(1); // 2 -> 1로 감소
        assertThat(homeData.popup().showPopup()).isTrue();
        assertThat(homeData.popup().failedMemberNames()).containsExactly(user.getNickname());
    }

    @Test
    @DisplayName("디톡스 시간이 null인 유저가 홈을 조회하면 MISSION_ERROR_400_DETOX_TIME_NOT_SET 예외가 발생한다")
    void homeDataThrowsWhenDetoxTimeNull() {
        // given
        User user = userRepository.save(User.builder()
                .deviceId(UUID.randomUUID().toString())
                .nickname("꺽정")
                .build());
        
        // when & then
        org.junit.jupiter.api.Assertions.assertThrows(
                com.example.hackathon.global.exception.BusinessException.class,
                () -> userService.getHomeData(user.getId())
        );
    }

    @Test
    @DisplayName("유저 상세 조회 시 등록 정보가 정상 반환된다")
    void getUserDetailsSuccess() {
        // given
        User user = createUser("길동", "test@test.com");
        
        // when
        UserResponse response = userService.getUser(user.getId());
        
        // then
        assertThat(response.id()).isEqualTo(user.getId());
        assertThat(response.nickname()).isEqualTo("길동");
        assertThat(response.email()).isEqualTo("test@test.com");
        assertThat(response.emailNotificationEnabled()).isTrue();
    }

    @Test
    @DisplayName("알림 토글 API 호출 시 수신동의 플래그가 정상 업데이트된다")
    void updateNotificationSettingSuccess() {
        // given
        User user = createUser("길동", "test@test.com");
        
        // when
        userService.updateNotificationSetting(user.getId(), false);
        
        // then
        User updated = userRepository.findById(user.getId()).orElseThrow();
        assertThat(updated.isEmailNotificationEnabled()).isFalse();
    }
}
