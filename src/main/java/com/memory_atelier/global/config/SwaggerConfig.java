package com.memory_atelier.global.config;

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
                        .title("메모리 아뜰리에 API")
                        .description("메모리 아뜰리에 API 명세")
                        .version("v0.0.1"));
    }
}
