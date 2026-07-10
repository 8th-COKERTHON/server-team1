package com.example.hackathon.global.config;

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
                        .title("적(積) API")
                        .description("랜덤 미션 기반 디지털 디톡스 서비스 · 제8회 코커톤")
                        .version("v1"));
    }
}
