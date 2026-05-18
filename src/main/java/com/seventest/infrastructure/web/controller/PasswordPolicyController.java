package com.seventest.infrastructure.web.controller;

import com.seventest.domain.model.PasswordPolicy;
import com.seventest.domain.port.in.PasswordPolicyUseCase;
import com.seventest.infrastructure.web.dto.request.PasswordPolicyRequest;
import com.seventest.infrastructure.web.dto.response.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Configuración", description = "Configuración de la política de contraseñas. Solo ADMINISTRADOR.")
@SecurityRequirement(name = "Bearer Auth")
@RestController
@RequestMapping("/api/config/password-policy")
@RequiredArgsConstructor
public class PasswordPolicyController {

    private final PasswordPolicyUseCase passwordPolicyUseCase;

    @Operation(summary = "Obtener política de contraseñas vigente",
            description = "Devuelve los parámetros de la política activa. "
                    + "Si nunca fue configurada, retorna los valores por defecto "
                    + "(longitud mínima 4, sin exigencias adicionales).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Política obtenida",
                    content = @Content(schema = @Schema(implementation = PasswordPolicy.class))),
            @ApiResponse(responseCode = "403", description = "Sin autorización o rol insuficiente")
    })
    @GetMapping
    public ResponseEntity<PasswordPolicy> get() {
        return ResponseEntity.ok(passwordPolicyUseCase.get());
    }

    @Operation(summary = "Actualizar política de contraseñas",
            description = "Reemplaza la política vigente. Los cambios se aplican inmediatamente "
                    + "a la creación y modificación de contraseñas. "
                    + "Las contraseñas existentes no se invalidan.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Política actualizada",
                    content = @Content(schema = @Schema(implementation = PasswordPolicy.class))),
            @ApiResponse(responseCode = "400", description = "Parámetros inválidos (ej. mínimo mayor que máximo)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Sin autorización o rol insuficiente")
    })
    @PutMapping
    public ResponseEntity<PasswordPolicy> update(@Valid @RequestBody PasswordPolicyRequest request) {
        PasswordPolicy policy = PasswordPolicy.builder()
                .minLength(request.minLength())
                .maxLength(request.maxLength())
                .requireUppercase(request.requireUppercase())
                .requireLowercase(request.requireLowercase())
                .requireNumbers(request.requireNumbers())
                .requireSpecialChars(request.requireSpecialChars())
                .build();
        return ResponseEntity.ok(passwordPolicyUseCase.update(policy));
    }
}
