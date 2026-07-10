package com.example.hackathon.domain.user.service;

import com.example.hackathon.domain.user.entity.User;
import com.example.hackathon.domain.user.repository.UserRepository;
import com.example.hackathon.domain.user.dto.response.UserCreateResponse;
import com.example.hackathon.domain.team.entity.Team;
import com.example.hackathon.domain.team.repository.TeamRepository;
import com.example.hackathon.domain.team.repository.TeamMemberRepository;
import com.example.hackathon.global.exception.BusinessException;
import com.example.hackathon.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {
    private final UserRepository userRepository;
    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;

    @Transactional
    public Long createUser(String deviceId, String nickname) {
        User user = User.builder()
                .deviceId(deviceId)
                .nickname(nickname)
                .detoxStartTime(LocalTime.of(0, 0))
                .detoxEndTime(LocalTime.of(0, 0))
                .build();
        return userRepository.save(user).getId();
    }

    @Transactional
    public UserCreateResponse getOrCreateUser(String deviceId, String nickname) {
        Optional<User> existingUser = userRepository.findByDeviceId(deviceId);
        if (existingUser.isPresent()) {
            User user = existingUser.get();
            return UserCreateResponse.of(user.getId(), user.getNickname(), "LOGIN");
        }
        
        User user = User.builder()
                .deviceId(deviceId)
                .nickname(nickname)
                .detoxStartTime(LocalTime.of(0, 0))
                .detoxEndTime(LocalTime.of(0, 0))
                .build();
        User savedUser = userRepository.save(user);
        return UserCreateResponse.of(savedUser.getId(), savedUser.getNickname(), "SIGN_UP");
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

    public Map<String, String> getDetoxTime(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("유저를 찾을 수 없습니다."));
        
        return Map.of(
            "startTime", user.getDetoxStartTime().toString(),
            "endTime", user.getDetoxEndTime().toString()
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