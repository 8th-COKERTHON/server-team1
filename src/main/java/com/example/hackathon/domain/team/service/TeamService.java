package com.example.hackathon.domain.team.service;

import com.example.hackathon.domain.team.dto.*;
import com.example.hackathon.domain.team.entity.Team;
import com.example.hackathon.domain.team.entity.TeamMember;
import com.example.hackathon.domain.team.repository.TeamMemberRepository;
import com.example.hackathon.domain.team.repository.TeamRepository;
import com.example.hackathon.domain.user.entity.User;
import com.example.hackathon.domain.user.repository.UserRepository;
import com.example.hackathon.global.exception.BusinessException;
import com.example.hackathon.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 팀은 N:M. 한 사용자가 여러 팀에 속할 수 있고, 소속은 TeamMember 가 표현한다.
 * 개인 모드는 팀이 아니므로 여기서 다루지 않는다 (개인 벽돌은 User 도메인).
 */
@Service
@RequiredArgsConstructor
public class TeamService {

    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final UserRepository userRepository;
    private final InviteCodeGenerator inviteCodeGenerator;

    @Transactional
    public TeamCreateResponse create(TeamCreateRequest request) {
        User user = findUser(request.userId());

        Team team = teamRepository.save(Team.builder()
                .name(request.teamName())
                .inviteCode(inviteCodeGenerator.generate())
                .build());

        // 생성자는 자동으로 팀원이 된다. N:M 이라 이미 다른 팀에 있어도 무관.
        teamMemberRepository.save(TeamMember.of(user, team));

        return TeamCreateResponse.from(team);
    }

    @Transactional
    public TeamJoinResponse join(TeamJoinRequest request) {
        User user = findUser(request.userId());

        // 팀 row 를 잠근 뒤 정원을 센다. 잠그지 않으면 동시 참여로 정원을 넘길 수 있다.
        Team team = teamRepository.findByInviteCodeForUpdate(request.inviteCode())
                .orElseThrow(() -> new BusinessException(ErrorCode.TEAM_ERROR_404_INVALID_INVITE_CODE));

        if (teamMemberRepository.existsByUserAndTeam(user, team)) {
            throw new BusinessException(ErrorCode.TEAM_ERROR_409_ALREADY_IN_TEAM);
        }

        int memberCount = teamMemberRepository.countByTeam(team);
        if (memberCount >= Team.MAX_MEMBERS) {
            throw new BusinessException(ErrorCode.TEAM_ERROR_409_TEAM_FULL);
        }

        teamMemberRepository.save(TeamMember.of(user, team));

        return TeamJoinResponse.of(team, memberCount + 1);
    }

    @Transactional(readOnly = true)
    public MyTeamResponse getMyTeams(Long userId) {
        User user = findUser(userId);

        List<MyTeamResponse.Item> items = teamMemberRepository.findByUser(user).stream()
                .map(TeamMember::getTeam)
                .map(team -> MyTeamResponse.Item.of(team, teamMemberRepository.countByTeam(team)))
                .toList();

        return MyTeamResponse.of(items);
    }

    @Transactional(readOnly = true)
    public TeamDetailResponse getTeamDetail(Long teamId) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TEAM_ERROR_404_NOT_FOUND));

        List<TeamMember> members = teamMemberRepository.findByTeam(team);

        return TeamDetailResponse.of(team, members);
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_ERROR_404_NOT_FOUND));
    }
}
