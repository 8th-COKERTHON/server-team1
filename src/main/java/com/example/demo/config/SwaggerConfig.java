package com.example.demo.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("🏆 해커톤 Team API") // Swagger UI 상단에 표시될 문서 제목
                        .description("프론트엔드 화이팅! 🚀") // 문서에 대한 간단한 설명
                        .version("v1.0")); // API 버전
    }
}