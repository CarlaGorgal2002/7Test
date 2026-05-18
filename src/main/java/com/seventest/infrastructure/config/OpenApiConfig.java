package com.seventest.infrastructure.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String SCHEME_NAME = "Bearer Auth";

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("7test API")
                        .description("""
                                API REST del Sistema de Gestión de Evaluaciones Universitarias.

                                **Autenticación:** usar el endpoint `/api/auth/login` para obtener un token JWT \
                                e ingresarlo en el botón **Authorize** con el formato `Bearer <token>`.

                                **Usuario admin inicial:** `admin@seventest.local` / `admin1234`
                                """)
                        .version("1.0.0 — Milestone 1"))
                .addSecurityItem(new SecurityRequirement().addList(SCHEME_NAME))
                .components(new Components()
                        .addSecuritySchemes(SCHEME_NAME, new SecurityScheme()
                                .name(SCHEME_NAME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Token JWT obtenido desde /api/auth/login")));
    }
}
