package com.example.hackathon.domain.user.service;

import com.example.hackathon.domain.user.entity.User;
import com.example.hackathon.domain.user.repository.UserRepository;
import com.example.hackathon.domain.user.dto.response.UserCreateResponse;
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