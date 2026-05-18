package com.seventest.infrastructure.web.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @Schema(description = "Email institucional del usuario", example = "admin@seventest.local")
        @Email @NotBlank String email,

        @Schema(description = "Contraseña del usuario", example = "admin1234")
        @NotBlank String password
) {}
