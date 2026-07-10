package com.example.hackathon.domain.user.service;

import com.example.hackathon.domain.user.entity.User;
import com.example.hackathon.domain.user.repository.UserRepository;
import com.example.hackathon.domain.user.dto.response.UserCreateResponse;
import com.example.hackathon.domain.user.dto.response.UserHomeResponse;
import com.example.hackathon.domain.user.dto.response.UserResponse;
import com.example.hackathon.domain.user.dto.response.UserHomeResponse.HomeMemberStatus;
import com.example.hackathon.domain.user.dto.response.UserHomeResponse.HomePopupInfo;
import com.example.hackathon.domain.user.entity.DailySettlementLog;
import com.example.hackathon.domain.user.repository.DailySettlementLogRepository;
import com.example.hackathon.domain.mission.entity.UserMissionLog;
import com.example.hackathon.domain.mission.entity.MissionStatus;
import com.example.hackathon.domain.mission.repository.UserMissionLogRepository;
import com.example.hackathon.domain.team.entity.Team;
import com.example.hackathon.domain.team.entity.TeamMember;
import com.example.hackathon.domain.team.repository.TeamRepository;
import com.example.hackathon.domain.team.repository.TeamMemberRepository;
import com.example.hackathon.global.exception.BusinessException;
import com.example.hackathon.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    private final UserRepository userRepository;
    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final DailySettlementLogRepository dailySettlementLogRepository;
    private final UserMissionLogRepository userMissionLogRepository;

    @Transactional
    public Long createUser(String deviceId, String nickname, String email) {
        User user = User.builder()
                .deviceId(deviceId)
                .nickname(nickname)
                .email(email)
                .build();
        return userRepository.save(user).getId();
    }

    @Transactional
    public UserCreateResponse getOrCreateUser(String deviceId, String nickname, String email) {
        Optional<User> existingUser = userRepository.findByDeviceId(deviceId);
        if (existingUser.isPresent()) {
            User user = existingUser.get();
            return UserCreateResponse.of(user.getId(), user.getNickname(), "LOGIN",
                    user.getSelectedTeam() != null ? user.getSelectedTeam().getId() : null, user.getEmail());
        }
        
        User user = User.builder()
                .deviceId(deviceId)
                .nickname(nickname)
                .email(email)
                .build();
        User savedUser = userRepository.save(user);
        return UserCreateResponse.of(savedUser.getId(), savedUser.getNickname(), "SIGN_UP", null, savedUser.getEmail());
    }

    @Transactional
    public void setDetoxTime(Long userId, String startTime, String endTime) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("유저를 찾을 수 없습니다."));
        
        user.updateDetoxTime(parseTime(startTime), parseTime(endTime));
    }

    @Transactional
    public void selectActiveTeam(Long userId, Long teamId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_ERROR_404_NOT_FOUND));

        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TEAM_ERROR_404_NOT_FOUND));

        if (!teamMemberRepository.existsByUserAndTeam(user, team)) {
            throw new BusinessException(ErrorCode.COMMON_ERROR_400_INVALID_INPUT, "해당 팀의 멤버가 아닙니다.");
        }

        user.selectTeam(team);
    }

    @Transactional
    public void updateUserEmail(Long userId, String email) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_ERROR_404_NOT_FOUND));
        user.updateEmail(email);
    }

    @Transactional(readOnly = true)
    public UserResponse getUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_ERROR_404_NOT_FOUND));
        return UserResponse.of(user.getId(), user.getNickname(), user.getEmail(), user.isEmailNotificationEnabled());
    }

    @Transactional
    public void updateNotificationSetting(Long userId, boolean enabled) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_ERROR_404_NOT_FOUND));
        user.updateEmailNotificationEnabled(enabled);
    }

    @Transactional
    public UserHomeResponse getHomeData(Long userId) {
        User user = userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_ERROR_404_NOT_FOUND));

        // 2. 디톡스 시간 미설정 시 명시적 예외 처리 (400 Bad Request)
        if (user.getDetoxStartTime() == null || user.getDetoxEndTime() == null) {
            throw new BusinessException(ErrorCode.MISSION_ERROR_400_DETOX_TIME_NOT_SET);
        }

        // 3. KST 시간대 고정
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));
        LocalDate yesterday = today.minusDays(1);
        
        Team selectedTeam = user.getSelectedTeam();

        // 4. Catch-up 정산 알고리즘 도입 (누락일 자동 순차 정산)
        LocalDate lastSettlement = user.getLastSettlementDate();
        if (lastSettlement.isBefore(yesterday)) {
            LocalDate targetDate = lastSettlement.plusDays(1);
            while (!targetDate.isAfter(yesterday)) {
                if (selectedTeam == null) {
                    // 개인 모드 정산
                    boolean existsLog = dailySettlementLogRepository.existsByUserIdAndTargetDate(userId, targetDate);
                    if (!existsLog) {
                        Optional<UserMissionLog> logOpt = userMissionLogRepository.findByUserIdAndTargetDate(userId, targetDate);
                        boolean isSuccess = logOpt.isPresent() && logOpt.get().getStatus() == MissionStatus.SUCCESS;
                        
                        int brickBefore = user.getPersonalBricks();
                        user.updatePersonalBricks(isSuccess ? 1 : -1);
                        int brickAfter = user.getPersonalBricks();

                        dailySettlementLogRepository.save(DailySettlementLog.builder()
                                .userId(userId)
                                .targetDate(targetDate)
                                .success(isSuccess)
                                .brickBefore(brickBefore)
                                .brickAfter(brickAfter)
                                .build());
                    }
                } else {
                    // 팀 모드 정산
                    Long teamId = selectedTeam.getId();
                    Team team = teamRepository.findByIdForUpdate(teamId)
                            .orElseThrow(() -> new BusinessException(ErrorCode.TEAM_ERROR_404_NOT_FOUND));

                    boolean existsLog = dailySettlementLogRepository.existsByTeamIdAndTargetDate(teamId, targetDate);
                    if (!existsLog) {
                        List<TeamMember> teamMembers = teamMemberRepository.findByTeam(team);
                        List<UserMissionLog> targetLogs = userMissionLogRepository.findByTeamIdAndTargetDate(teamId, targetDate);

                        // targetDate 당일 혹은 이전에 가입한 액티브 멤버만 필터링
                        List<TeamMember> activeMembers = new ArrayList<>();
                        for (TeamMember tm : teamMembers) {
                            if (!tm.getJoinedAt().toLocalDate().isAfter(targetDate)) {
                                activeMembers.add(tm);
                            }
                        }

                        boolean allSuccess = true;
                        if (activeMembers.isEmpty()) {
                            allSuccess = false;
                        } else {
                            for (TeamMember tm : activeMembers) {
                                Optional<UserMissionLog> logOpt = targetLogs.stream()
                                        .filter(l -> l.getUser().getId().equals(tm.getUser().getId()))
                                        .findFirst();
                                if (logOpt.isEmpty() || logOpt.get().getStatus() != MissionStatus.SUCCESS) {
                                    allSuccess = false;
                                    break;
                                }
                            }
                        }

                        int brickBefore = team.getTotalBricks();
                        team.updateTotalBricks(allSuccess ? 1 : -1);
                        int brickAfter = team.getTotalBricks();

                        dailySettlementLogRepository.save(DailySettlementLog.builder()
                                .teamId(teamId)
                                .userId(userId)
                                .targetDate(targetDate)
                                .success(allSuccess)
                                .brickBefore(brickBefore)
                                .brickAfter(brickAfter)
                                .build());
                    }
                }
                targetDate = targetDate.plusDays(1);
            }
            user.updateLastSettlementDate(yesterday);
        }

        // 5. 팝업 데이터 셋팅 (개별 유저 기준 1일 1회 표시 및 실패자 닉네임 리스트 반환)
        boolean showPopup = false;
        List<String> failedMemberNames = new ArrayList<>();

        if (user.getLastPopupShownDate() == null || !user.getLastPopupShownDate().isEqual(today)) {
            if (selectedTeam == null) {
                Optional<UserMissionLog> yesterdayLog = userMissionLogRepository.findByUserIdAndTargetDate(userId, yesterday);
                boolean yesterdaySuccess = yesterdayLog.isPresent() && yesterdayLog.get().getStatus() == MissionStatus.SUCCESS;
                if (!yesterdaySuccess) {
                    showPopup = true;
                    failedMemberNames.add(user.getNickname());
                    user.updateLastPopupShownDate(today);
                }
            } else {
                Team team = teamRepository.findById(selectedTeam.getId())
                        .orElseThrow(() -> new BusinessException(ErrorCode.TEAM_ERROR_404_NOT_FOUND));
                List<TeamMember> teamMembers = teamMemberRepository.findByTeam(team);
                List<UserMissionLog> yesterdayLogs = userMissionLogRepository.findByTeamIdAndTargetDate(team.getId(), yesterday);

                for (TeamMember tm : teamMembers) {
                    if (!tm.getJoinedAt().toLocalDate().isAfter(yesterday)) {
                        Optional<UserMissionLog> logOpt = yesterdayLogs.stream()
                                .filter(l -> l.getUser().getId().equals(tm.getUser().getId()))
                                .findFirst();
                        if (logOpt.isEmpty() || logOpt.get().getStatus() != MissionStatus.SUCCESS) {
                            failedMemberNames.add(tm.getUser().getNickname());
                        }
                    }
                }
                if (!failedMemberNames.isEmpty()) {
                    showPopup = true;
                    user.updateLastPopupShownDate(today);
                }
            }
        }

        // 6. 응답 객체 빌드
        if (selectedTeam == null) {
            Optional<UserMissionLog> yesterdayLog = userMissionLogRepository.findByUserIdAndTargetDate(userId, yesterday);
            boolean isSuccess = yesterdayLog.isPresent() && yesterdayLog.get().getStatus() == MissionStatus.SUCCESS;
            String imageUrl = yesterdayLog.isPresent() ? yesterdayLog.get().getImageUrl() : null;

            List<HomeMemberStatus> members = List.of(new HomeMemberStatus(
                    user.getId(),
                    user.getNickname(),
                    isSuccess,
                    imageUrl
            ));

            return new UserHomeResponse(
                    "INDIVIDUAL",
                    user.getDetoxStartTime().format(TIME_FORMATTER),
                    user.getDetoxEndTime().format(TIME_FORMATTER),
                    null,
                    null,
                    user.getPersonalBricks(),
                    user.getPersonalBricks() / 10 + 1,
                    members,
                    new HomePopupInfo(showPopup, failedMemberNames)
            );
        } else {
            Team team = teamRepository.findById(selectedTeam.getId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.TEAM_ERROR_404_NOT_FOUND));
            List<TeamMember> teamMembers = teamMemberRepository.findByTeam(team);
            List<UserMissionLog> yesterdayLogs = userMissionLogRepository.findByTeamIdAndTargetDate(team.getId(), yesterday);

            List<HomeMemberStatus> members = teamMembers.stream().map(tm -> {
                Optional<UserMissionLog> logOpt = yesterdayLogs.stream()
                        .filter(l -> l.getUser().getId().equals(tm.getUser().getId()))
                        .findFirst();
                boolean isSuccess = logOpt.isPresent() && logOpt.get().getStatus() == MissionStatus.SUCCESS;
                String imageUrl = logOpt.isPresent() ? logOpt.get().getImageUrl() : null;
                return new HomeMemberStatus(
                        tm.getUser().getId(),
                        tm.getUser().getNickname(),
                        isSuccess,
                        imageUrl
                );
            }).collect(Collectors.toList());

            return new UserHomeResponse(
                    "TEAM",
                    user.getDetoxStartTime().format(TIME_FORMATTER),
                    user.getDetoxEndTime().format(TIME_FORMATTER),
                    team.getId(),
                    team.getName(),
                    team.getTotalBricks(),
                    team.getTotalBricks() / 10 + 1,
                    members,
                    new HomePopupInfo(showPopup, failedMemberNames)
            );
        }
    }

    public Map<String, String> getDetoxTime(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("유저를 찾을 수 없습니다."));
        return Map.of(
                "startTime", user.getDetoxStartTime() != null ? user.getDetoxStartTime().format(TIME_FORMATTER) : "00:00",
                "endTime", user.getDetoxEndTime() != null ? user.getDetoxEndTime().format(TIME_FORMATTER) : "00:00"
        );
    }

    @Transactional
    public void updateDetoxTime(Long userId, String startTime, String endTime) {
        setDetoxTime(userId, startTime, endTime);
    }

    private LocalTime parseTime(String time) {
        return LocalTime.parse(time, DateTimeFormatter.ofPattern("HH:mm"));
    }
}