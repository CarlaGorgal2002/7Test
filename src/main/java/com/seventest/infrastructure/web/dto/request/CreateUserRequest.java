package com.seventest.infrastructure.web.dto.request;

import com.seventest.domain.model.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateUserRequest(
        @Schema(description = "Nombre completo del usuario", example = "María García")
        @NotBlank String fullName,

        @Schema(description = "Email institucional — debe ser único en la plataforma", example = "maria@uade.edu.ar")
        @Email @NotBlank String email,

        @Schema(description = "Rol asignado al usuario")
        @NotNull Role role,

        @Schema(description = "Contraseña inicial — debe cumplir la política de contraseñas vigente", example = "Pass1234")
        @NotBlank String initialPassword
) {}
