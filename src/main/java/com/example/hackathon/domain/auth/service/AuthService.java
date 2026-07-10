package com.example.hackathon.domain.auth.service;

import com.example.hackathon.domain.auth.dto.LoginRequest;
import com.example.hackathon.domain.auth.dto.SignupRequest;
import com.example.hackathon.domain.auth.dto.TokenResponse;
import com.example.hackathon.domain.auth.oauth.GoogleOAuthClient;
import com.example.hackathon.domain.auth.oauth.KakaoOAuthClient;
import com.example.hackathon.domain.auth.oauth.OAuthUserInfo;
import com.example.hackathon.domain.user.entity.Provider;
import com.example.hackathon.domain.user.entity.Role;
import com.example.hackathon.domain.user.entity.User;
import com.example.hackathon.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final com.example.hackathon.global.jwt.JwtProvider jwtProvider;
    private final KakaoOAuthClient kakaoOAuthClient;
    private final GoogleOAuthClient googleOAuthClient;

    // 회원가입
    public TokenResponse signup(SignupRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("이미 가입된 이메일입니다.");
        }
        User user = userRepository.save(User.builder()
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .nickname(request.nickname())
                .provider(Provider.LOCAL)
                .role(Role.USER)
                .build());
        return issueToken(user);
    }

    // 로그인
    @Transactional(readOnly = true)
    public TokenResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new IllegalArgumentException("이메일 또는 비밀번호가 올바르지 않습니다."));

        if (user.getPassword() == null
                || !passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new IllegalArgumentException("이메일 또는 비밀번호가 올바르지 않습니다.");
        }
        return issueToken(user);
    }

    // 카카오 로그인
    public TokenResponse kakaoLogin(String code, String redirectUri) {
        return socialLogin(kakaoOAuthClient.getUserInfo(code, redirectUri));
    }

    // 구글 로그인
    public TokenResponse googleLogin(String code, String redirectUri) {
        return socialLogin(googleOAuthClient.getUserInfo(code, redirectUri));
    }

    // 소셜 로그인 공통: 있으면 로그인, 없으면 가입 후 로그인
    private TokenResponse socialLogin(OAuthUserInfo info) {
        User user = userRepository.findByProviderAndProviderId(info.provider(), info.providerId())
                .orElseGet(() -> userRepository.save(User.builder()
                        .email(resolveEmail(info))
                        .nickname(info.nickname())
                        .provider(info.provider())
                        .providerId(info.providerId())
                        .role(Role.USER)
                        .build()));
        return issueToken(user);
    }

    // 소셜에서 이메일 동의를 안 했을 경우 대비한 대체 이메일
    private String resolveEmail(OAuthUserInfo info) {
        if (info.email() != null && !info.email().isBlank()) {
            return info.email();
        }
        return info.provider().name().toLowerCase() + "_" + info.providerId() + "@social.local";
    }

    private TokenResponse issueToken(User user) {
        String token = jwtProvider.createToken(user.getId(), user.getEmail(), user.getRole().name());
        return TokenResponse.of(token);
    }
}
