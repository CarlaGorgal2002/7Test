package com.seventest.infrastructure.web.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record PasswordPolicyRequest(
        @Schema(description = "Longitud mínima de la contraseña", example = "8")
        @Min(1) int minLength,

        @Schema(description = "Longitud máxima de la contraseña", example = "50")
        @Min(1) int maxLength,

        @Schema(description = "Exigir al menos una letra mayúscula", example = "true")
        @NotNull boolean requireUppercase,

        @Schema(description = "Exigir al menos una letra minúscula", example = "false")
        @NotNull boolean requireLowercase,

        @Schema(description = "Exigir al menos un número", example = "true")
        @NotNull boolean requireNumbers,

        @Schema(description = "Exigir al menos un carácter especial", example = "false")
        @NotNull boolean requireSpecialChars
) {}
