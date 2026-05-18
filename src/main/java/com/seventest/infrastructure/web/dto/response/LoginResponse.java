package com.seventest.infrastructure.web.dto.response;

import com.seventest.domain.model.Role;
import io.swagger.v3.oas.annotations.media.Schema;

public record LoginResponse(
        @Schema(description = "Token JWT para incluir en el header Authorization de las siguientes requests")
        String token,

        @Schema(description = "Rol del usuario autenticado — usado por el frontend para redirigir a la landing correspondiente")
        Role role,

        @Schema(description = "Nombre completo del usuario autenticado", example = "María García")
        String fullName
) {}
