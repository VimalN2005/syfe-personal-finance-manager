package com.syfe.financemanager.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Personal Finance Manager API")
                        .version("1.0.0")
                        .description("High-performance Personal Finance Management System API developed for Syfe Backend Assessment.")
                        .contact(new Contact()
                                .name("Syfe Candidate")
                                .email("candidate@syfe.com"))
                        .license(new License().name("Apache 2.0").url("https://springdoc.org")));
    }
}
