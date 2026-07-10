package com.example.demo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable()) // POST 요청 시 403 에러 방지
            .cors(cors -> cors.configure(http)) // WebConfig의 CORS 설정 연동
            .authorizeHttpRequests(auth -> auth
                .anyRequest().permitAll() // 일단 모든 API 통신을 다 열어둠 (해커톤 꼼수)
            );
        return http.build();
    }
}