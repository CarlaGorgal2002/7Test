package com.seventest.infrastructure.web.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record PasswordRecoveryRequest(
        @Schema(description = "Email del usuario que solicita la recuperación", example = "usuario@uade.edu.ar")
        @Email @NotBlank String email
) {}
