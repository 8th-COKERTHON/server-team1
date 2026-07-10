package com.example.hackathon.domain.team.repository;

import com.example.hackathon.domain.team.entity.Team;
import com.example.hackathon.domain.team.entity.TeamMember;
import com.example.hackathon.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TeamMemberRepository extends JpaRepository<TeamMember, Long> {

    boolean existsByUserAndTeam(User user, Team team);

    int countByTeam(Team team);

    /** 내가 속한 팀 목록 (팀 전환 바텀시트용). */
    List<TeamMember> findByUser(User user);

    /** 팀의 구성원 목록 (팀 상세용). */
    List<TeamMember> findByTeam(Team team);

    @Query("select distinct tm.user from TeamMember tm " +
           "where tm.team.id in (select mine.team.id from TeamMember mine where mine.user.id = :userId) " +
           "and tm.user.id <> :userId")
    List<User> findDistinctTeammatesByUserId(@Param("userId") Long userId);
}
