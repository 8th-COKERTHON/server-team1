package com.example.hackathon.domain.team.service;

import com.example.hackathon.domain.team.dto.*;
import com.example.hackathon.domain.team.entity.Team;
import com.example.hackathon.domain.user.entity.User;
import com.example.hackathon.domain.user.repository.UserRepository;
import com.example.hackathon.global.exception.BusinessException;
import com.example.hackathon.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class TeamServiceTest {

    @Autowired
    private TeamService teamService;

    @Autowired
    private UserRepository userRepository;

    private User newUser() {
        return userRepository.save(User.builder()
                .deviceId(UUID.randomUUID().toString())
                .nickname("tester")
                .detoxStartTime(LocalTime.of(21, 0))
                .detoxEndTime(LocalTime.of(23, 0))
                .build());
    }

    @Test
    @DisplayName("팀을 만들면 초대코드가 발급되고 생성자가 팀원이 된다")
    void createTeam() {
        User user = newUser();

        TeamCreateResponse response = teamService.create(new TeamCreateRequest(user.getId(), "디톡스"));

        assertThat(response.teamId()).isNotNull();
        assertThat(response.teamName()).isEqualTo("디톡스");
        assertThat(response.inviteCode()).hasSize(6);

        MyTeamResponse myTeams = teamService.getMyTeams(user.getId());
        assertThat(myTeams.teams()).hasSize(1);
        assertThat(myTeams.teams().get(0).memberCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("한 사용자가 여러 팀에 동시에 속할 수 있다 (N:M)")
    void userCanBelongToMultipleTeams() {
        User user = newUser();

        teamService.create(new TeamCreateRequest(user.getId(), "팀A"));
        teamService.create(new TeamCreateRequest(user.getId(), "팀B"));

        MyTeamResponse myTeams = teamService.getMyTeams(user.getId());
        assertThat(myTeams.teams()).hasSize(2);
        assertThat(myTeams.teams()).extracting(MyTeamResponse.Item::teamName)
                .containsExactlyInAnyOrder("팀A", "팀B");
    }

    @Test
    @DisplayName("존재하지 않는 사용자로는 팀을 만들 수 없다")
    void createTeam_userNotFound() {
        assertThatThrownBy(() -> teamService.create(new TeamCreateRequest(999_999L, "없는사람")))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.USER_ERROR_404_NOT_FOUND);
    }

    @Test
    @DisplayName("초대코드로 참여하면 인원 수가 늘어난다")
    void joinTeam() {
        User owner = newUser();
        String code = teamService.create(new TeamCreateRequest(owner.getId(), "팀")).inviteCode();

        User joiner = newUser();
        TeamJoinResponse response = teamService.join(new TeamJoinRequest(joiner.getId(), code));

        assertThat(response.memberCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("없는 초대코드로 참여하면 실패한다")
    void joinTeam_invalidCode() {
        User user = newUser();

        assertThatThrownBy(() -> teamService.join(new TeamJoinRequest(user.getId(), "ZZZZZZ")))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.TEAM_ERROR_404_INVALID_INVITE_CODE);
    }

    @Test
    @DisplayName("같은 팀에 두 번 참여할 수 없다")
    void joinTeam_alreadyMember() {
        User owner = newUser();
        String code = teamService.create(new TeamCreateRequest(owner.getId(), "팀")).inviteCode();

        User joiner = newUser();
        teamService.join(new TeamJoinRequest(joiner.getId(), code));

        assertThatThrownBy(() -> teamService.join(new TeamJoinRequest(joiner.getId(), code)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.TEAM_ERROR_409_ALREADY_IN_TEAM);
    }

    @Test
    @DisplayName("정원 4명이 차면 더 참여할 수 없다")
    void joinTeam_full() {
        User owner = newUser();
        String code = teamService.create(new TeamCreateRequest(owner.getId(), "팀")).inviteCode();

        for (int i = 0; i < Team.MAX_MEMBERS - 1; i++) {
            teamService.join(new TeamJoinRequest(newUser().getId(), code));
        }

        User fifth = newUser();
        assertThatThrownBy(() -> teamService.join(new TeamJoinRequest(fifth.getId(), code)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.TEAM_ERROR_409_TEAM_FULL);
    }

    @Test
    @DisplayName("팀 상세는 팀원 목록과 벽돌/단계를 반환한다")
    void teamDetail() {
        User owner = newUser();
        Long teamId = teamService.create(new TeamCreateRequest(owner.getId(), "팀")).teamId();
        teamService.join(new TeamJoinRequest(newUser().getId(),
                teamService.getTeamDetail(teamId).inviteCode()));

        TeamDetailResponse detail = teamService.getTeamDetail(teamId);

        assertThat(detail.members()).hasSize(2);
        assertThat(detail.totalBricks()).isZero();
        assertThat(detail.stage()).isEqualTo(1);
    }

    @Test
    @DisplayName("존재하지 않는 팀 상세 조회는 실패한다")
    void teamDetail_notFound() {
        assertThatThrownBy(() -> teamService.getTeamDetail(999_999L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.TEAM_ERROR_404_NOT_FOUND);
    }
}
