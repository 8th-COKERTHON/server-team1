package com.example.hackathon.domain.sample;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * [해커톤 가이드 예시] Service/Controller 하드코딩 Mock 데이터 패턴.
 *
 * DB 연동이 꼬이거나 데이터가 없을 때, 주저 없이 이렇게 Mock 데이터를 리턴해서
 * 프론트엔드 화면 구현/시연을 먼저 진행한다. 실제 배포 시 Repository 연동으로 교체.
 *
 * 참고: 이 엔드포인트는 인증 없이 접근 가능하도록 SecurityConfig 에 넣지 않았으므로
 *       토큰이 필요하다. 데모용으로 열고 싶으면 PUBLIC_PATHS 에 "/api/sample/**" 추가.
 */
@Tag(name = "Sample", description = "하드코딩 Mock 데이터 예시")
@RestController
@RequestMapping("/api/sample")
public class SampleController {

    public record ApplicationListResponse(Long id, String name, String part, String status) {
    }

    @Operation(summary = "지원자 목록 (Mock)", description = "DB 없이 하드코딩 데이터를 반환하는 예시")
    @GetMapping("/applications")
    public List<ApplicationListResponse> getApplicationList() {
        // [REAL] 실제 배포 시에는 Repository 연동으로 교체
        // return applicationRepository.findAll().stream().map(...).toList();

        // [HACKATHON] 프론트 화면 구현을 위한 Mock 데이터
        return List.of(
                new ApplicationListResponse(1L, "김철수", "백엔드", "제출완료"),
                new ApplicationListResponse(2L, "이영희", "프론트엔드", "작성중"),
                new ApplicationListResponse(3L, "박해커", "디자인", "심사중"),
                new ApplicationListResponse(4L, "최데모", "기획", "불합격")
        );
    }
}
