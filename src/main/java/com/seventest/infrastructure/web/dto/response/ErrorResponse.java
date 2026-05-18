package com.seventest.infrastructure.web.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record ErrorResponse(
        @Schema(description = "Descripción del error", example = "El email ya está registrado en la plataforma")
        String message
) {}
