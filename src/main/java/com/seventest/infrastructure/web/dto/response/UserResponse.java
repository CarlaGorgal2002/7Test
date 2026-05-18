package com.seventest.infrastructure.web.dto.response;

import com.seventest.domain.model.Role;
import com.seventest.domain.model.UserStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

public record UserResponse(
        @Schema(description = "Identificador único del usuario")
        UUID id,

        @Schema(description = "Nombre completo", example = "María García")
        String fullName,

        @Schema(description = "Email institucional", example = "maria@uade.edu.ar")
        String email,

        @Schema(description = "Rol asignado")
        Role role,

        @Schema(description = "Estado de la cuenta")
        UserStatus status
) {}
