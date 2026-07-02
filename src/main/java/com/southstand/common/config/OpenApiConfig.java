package com.southstand.common.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI southStandOpenApi(@Value("${app.version:0.1.0-SNAPSHOT}") String version) {
        return new OpenAPI()
                .info(new Info()
                        .title("南看台 / Tifo API")
                        .description("South Stand backend API")
                        .version(version));
    }
}
