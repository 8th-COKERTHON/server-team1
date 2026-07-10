package com.example.hackathon.domain.auth.controller;

import com.example.hackathon.domain.auth.dto.LoginRequest;
import com.example.hackathon.domain.auth.dto.OAuthLoginRequest;
import com.example.hackathon.domain.auth.dto.SignupRequest;
import com.example.hackathon.domain.auth.dto.TokenResponse;
import com.example.hackathon.domain.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Auth", description = "인증 API (회원가입 / 로그인 / 소셜로그인)")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "회원가입", description = "이메일/비밀번호/닉네임으로 가입하고 즉시 토큰을 발급합니다.")
    @PostMapping("/signup")
    public ResponseEntity<TokenResponse> signup(@Valid @RequestBody SignupRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.signup(request));
    }

    @Operation(summary = "로그인", description = "이메일/비밀번호로 로그인하고 Access Token 을 발급합니다.")
    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @Operation(summary = "카카오 로그인", description = "프론트에서 받은 인가코드(code)로 로그인/가입 후 토큰을 발급합니다.")
    @PostMapping("/oauth/kakao")
    public ResponseEntity<TokenResponse> kakaoLogin(@Valid @RequestBody OAuthLoginRequest request) {
        return ResponseEntity.ok(authService.kakaoLogin(request.code(), request.redirectUri()));
    }

    @Operation(summary = "구글 로그인", description = "프론트에서 받은 인가코드(code)로 로그인/가입 후 토큰을 발급합니다.")
    @PostMapping("/oauth/google")
    public ResponseEntity<TokenResponse> googleLogin(@Valid @RequestBody OAuthLoginRequest request) {
        return ResponseEntity.ok(authService.googleLogin(request.code(), request.redirectUri()));
    }
}
