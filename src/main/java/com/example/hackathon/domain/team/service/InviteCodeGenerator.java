package com.example.hackathon.domain.team.service;

import com.example.hackathon.domain.team.repository.TeamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
@RequiredArgsConstructor
public class InviteCodeGenerator {

    /**
     * 사람이 보고 옮겨 적는 코드라 헷갈리는 글자를 뺐다.
     * 0/O, 1/I 를 제외해서 "영은 오, 일은 아이" 로 오타가 나는 상황을 막는다.
     */
    private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int CODE_LENGTH = 6;
    private static final int MAX_ATTEMPTS = 10;

    private final SecureRandom random = new SecureRandom();
    private final TeamRepository teamRepository;

    /**
     * 중복되지 않는 초대코드를 만든다.
     * 32^6 ≈ 10억 가지라 실제로 부딪힐 일은 거의 없지만, 부딪히면 다시 뽑는다.
     * DB 의 unique 제약이 최종 방어선이다.
     */
    public String generate() {
        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            String code = randomCode();
            if (!teamRepository.existsByInviteCode(code)) {
                return code;
            }
        }
        throw new IllegalStateException("초대코드 생성에 %d 번 실패했다".formatted(MAX_ATTEMPTS));
    }

    private String randomCode() {
        StringBuilder sb = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            sb.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        }
        return sb.toString();
    }
}
