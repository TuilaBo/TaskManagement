package com.example.TaskManagement.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String BEARER_AUTH = "Bearer Authentication";

    @Bean
    public OpenAPI taskManagementOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Task Management API")
                        .description("""
                                REST API quản lý công việc với JWT authentication.

                                **Luồng sử dụng Swagger:**
                                1. Gọi `POST /api/auth/login` để lấy token (manager: manager/manager123)
                                2. Bấm nút **Authorize** (góc trên phải), chỉ nhập token JWT (không cần gõ chữ Bearer)
                                3. Gọi các API `/api/tasks/**`
                                """)
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Task Management")
                                .email("support@task.com")))
                .components(new Components()
                        .addSecuritySchemes(BEARER_AUTH, new SecurityScheme()
                                .name(BEARER_AUTH)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Nhập JWT token. Ví dụ: eyJhbGciOiJIUzI1NiJ9...")));
    }
}
