package com.example.hackathon.domain.team.repository;

import com.example.hackathon.domain.team.entity.Team;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface TeamRepository extends JpaRepository<Team, Long> {

    boolean existsByInviteCode(String inviteCode);

    /**
     * 팀 참여용 조회. 팀 row 에 쓰기 잠금을 건다.
     *
     * 정원 검사(count)와 TeamMember 추가 사이에 다른 요청이 끼어들면
     * 두 명이 동시에 "3명이네, 들어가도 되겠다" 를 통과해 5명이 될 수 있다.
     * 같은 팀을 노리는 요청들을 여기서 직렬화한다.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from Team t where t.inviteCode = :inviteCode")
    Optional<Team> findByInviteCodeForUpdate(@Param("inviteCode") String inviteCode);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from Team t where t.id = :id")
    Optional<Team> findByIdForUpdate(@Param("id") Long id);
}
