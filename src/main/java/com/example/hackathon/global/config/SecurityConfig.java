package com.example.hackathon.global.config;

import com.example.hackathon.global.jwt.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * 인증(auth)은 나중에 통째로 제거될 수 있다. 제거 절차는 README 의 "인증 제거" 항목 참고.
 * 그래서 인증에만 필요한 것들은 아래 [AUTH] 표시 블록에 모아둔다.
 * 새로 만드는 기능 엔드포인트는 기본이 공개(permitAll)이므로 인증 유무와 무관하게 동작한다.
 */
@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    // [AUTH] 인증 제거 시 이 필드와 addFilterBefore 한 줄을 지운다.
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Value("${cors.allowed-origins}")
    private List<String> allowedOrigins;

    // 토큰이 있어야만 접근 가능한 경로. 인증을 제거하면 이 배열째로 지운다.
    private static final String[] AUTHENTICATED_PATHS = {
            "/api/users/**"
    };

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // [AUTH] 인증 제거 시 이 한 줄만 지우면 된다.
                        .requestMatchers(AUTHENTICATED_PATHS).authenticated()
                        // 나머지는 전부 공개. 기능 개발이 로그인 완성을 기다리지 않게 한다.
                        .anyRequest().permitAll()
                )
                // [AUTH] 인증 제거 시 지운다.
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(allowedOrigins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setExposedHeaders(List.of("Authorization"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    // [AUTH] 비밀번호 해시. 인증 제거 시 함께 지운다.
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
