package com.example.hackathon.domain.detox.service;

import com.example.hackathon.domain.detox.dto.DetoxProgressResponse;
import com.example.hackathon.domain.detox.dto.OverlappingMemberResponse;
import com.example.hackathon.domain.mission.entity.MissionStatus;
import com.example.hackathon.domain.mission.entity.UserMissionLog;
import com.example.hackathon.domain.mission.repository.UserMissionLogRepository;
import com.example.hackathon.domain.team.repository.TeamMemberRepository;
import com.example.hackathon.domain.user.entity.User;
import com.example.hackathon.domain.user.repository.UserRepository;
import com.example.hackathon.global.exception.BusinessException;
import com.example.hackathon.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DetoxProgressService {

    private static final String PROGRESS_TITLE = "폰 내려놓을 시간이에요.";
    private static final String BEFORE_START_TITLE = "디지털 디톡스 시작 전이에요.";
    private static final String FINISHED_TITLE = "디지털 디톡스가 종료되었어요.";
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    private final UserRepository userRepository;
    private final UserMissionLogRepository userMissionLogRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final Clock clock;

    public DetoxProgressResponse getProgress(String deviceId) {
        LocalDateTime now = LocalDateTime.now(clock);
        User user = findUser(deviceId);
        UserMissionLog missionLog = findCurrentMissionLog(user, now.toLocalDate());
        validateMissionSuccess(missionLog);

        DetoxPeriod userPeriod = DetoxPeriod.of(
                missionLog.getTargetDate(), user.getDetoxStartTime(), user.getDetoxEndTime());
        boolean inProgress = userPeriod.contains(now);
        long remainingSeconds = calculateRemainingSeconds(userPeriod, now, inProgress);
        List<OverlappingMemberResponse> members = inProgress
                ? findOverlappingMembers(user, missionLog.getTargetDate(), userPeriod)
                : List.of();

        String titleMessage = resolveTitleMessage(userPeriod, now, inProgress);
        String unlockMessage = inProgress
                ? "%s에 해제됩니다.".formatted(user.getDetoxEndTime().format(TIME_FORMATTER))
                : null;

        return new DetoxProgressResponse(
                missionLog.getId(), missionLog.getStatus(), inProgress,
                userPeriod.start(), userPeriod.end(), user.getDetoxEndTime(), remainingSeconds,
                titleMessage, unlockMessage, members.size(), members
        );
    }

    private User findUser(String deviceId) {
        User user = userRepository.findByDeviceId(deviceId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_ERROR_404_NOT_FOUND));
        if (user.getDetoxStartTime() == null || user.getDetoxEndTime() == null) {
            throw new BusinessException(ErrorCode.MISSION_ERROR_400_DETOX_TIME_NOT_SET);
        }
        return user;
    }

    private UserMissionLog findCurrentMissionLog(User user, LocalDate today) {
        return userMissionLogRepository.findByUserIdAndTargetDate(user.getId(), today)
                .orElseGet(() -> {
                    if (!user.getDetoxEndTime().isAfter(user.getDetoxStartTime())) {
                        return userMissionLogRepository
                                .findByUserIdAndTargetDate(user.getId(), today.minusDays(1))
                                .orElseThrow(() -> new BusinessException(ErrorCode.MISSION_ERROR_404_NOT_FOUND));
                    }
                    throw new BusinessException(ErrorCode.MISSION_ERROR_404_NOT_FOUND);
                });
    }

    private void validateMissionSuccess(UserMissionLog missionLog) {
        if (missionLog.getStatus() != MissionStatus.SUCCESS) {
            throw new BusinessException(ErrorCode.MISSION_ERROR_400_NOT_CERTIFIED);
        }
    }

    private long calculateRemainingSeconds(DetoxPeriod period, LocalDateTime now, boolean inProgress) {
        return inProgress ? Math.max(Duration.between(now, period.end()).getSeconds(), 0) : 0;
    }

    private List<OverlappingMemberResponse> findOverlappingMembers(
            User user,
            LocalDate targetDate,
            DetoxPeriod userPeriod
    ) {
        Set<Long> seenUserIds = new HashSet<>();
        return teamMemberRepository.findDistinctTeammatesByUserId(user.getId()).stream()
                .filter(member -> !member.getId().equals(user.getId()))
                .filter(member -> seenUserIds.add(member.getId()))
                .filter(member -> member.getDetoxStartTime() != null && member.getDetoxEndTime() != null)
                .map(member -> toOverlappingMember(member, targetDate, userPeriod))
                .filter(java.util.Objects::nonNull)
                .sorted(Comparator.comparing(OverlappingMemberResponse::nickname)
                        .thenComparing(OverlappingMemberResponse::userId))
                .toList();
    }

    private OverlappingMemberResponse toOverlappingMember(
            User member,
            LocalDate targetDate,
            DetoxPeriod userPeriod
    ) {
        // 모든 팀원의 시작 시각을 현재 미션의 targetDate에 결합하는 정책을 사용한다.
        DetoxPeriod memberPeriod = DetoxPeriod.of(
                targetDate, member.getDetoxStartTime(), member.getDetoxEndTime());
        if (!userPeriod.overlaps(memberPeriod)) {
            return null;
        }
        DetoxPeriod overlap = userPeriod.intersection(memberPeriod);
        return new OverlappingMemberResponse(
                member.getId(), member.getNickname(),
                member.getDetoxStartTime(), member.getDetoxEndTime(),
                overlap.start(), overlap.end()
        );
    }

    private String resolveTitleMessage(DetoxPeriod period, LocalDateTime now, boolean inProgress) {
        if (inProgress) {
            return PROGRESS_TITLE;
        }
        return now.isBefore(period.start()) ? BEFORE_START_TITLE : FINISHED_TITLE;
    }

    private record DetoxPeriod(LocalDateTime start, LocalDateTime end) {

        private static DetoxPeriod of(LocalDate targetDate, LocalTime startTime, LocalTime endTime) {
            LocalDate endDate = endTime.isAfter(startTime) ? targetDate : targetDate.plusDays(1);
            return new DetoxPeriod(
                    LocalDateTime.of(targetDate, startTime),
                    LocalDateTime.of(endDate, endTime)
            );
        }

        private boolean contains(LocalDateTime dateTime) {
            return !dateTime.isBefore(start) && dateTime.isBefore(end);
        }

        private boolean overlaps(DetoxPeriod other) {
            return start.isBefore(other.end) && other.start.isBefore(end);
        }

        private DetoxPeriod intersection(DetoxPeriod other) {
            return new DetoxPeriod(
                    start.isAfter(other.start) ? start : other.start,
                    end.isBefore(other.end) ? end : other.end
            );
        }
    }
}
