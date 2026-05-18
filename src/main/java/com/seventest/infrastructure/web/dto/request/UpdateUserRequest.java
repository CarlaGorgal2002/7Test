package com.seventest.infrastructure.web.dto.request;

import com.seventest.domain.model.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UpdateUserRequest(
        @Schema(description = "Nombre completo del usuario", example = "María García Editada")
        @NotBlank String fullName,

        @Schema(description = "Email institucional", example = "maria@uade.edu.ar")
        @Email @NotBlank String email,

        @Schema(description = "Rol del usuario")
        @NotNull Role role,

        @Schema(description = "Nueva contraseña. Enviar null para no modificarla.", example = "NuevoPass99", nullable = true)
        String newPassword
) {}
