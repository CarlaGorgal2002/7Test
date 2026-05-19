package com.seventest.infrastructure.web.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record RecoverByNameRequest(
        @Schema(description = "Nombre completo del usuario", example = "Juan Perez")
        @NotBlank String name
) {}
