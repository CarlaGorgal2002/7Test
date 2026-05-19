package com.seventest.infrastructure.web.controller;

import com.seventest.domain.model.LoginResult;
import com.seventest.domain.port.in.AuthUseCase;
import com.seventest.infrastructure.web.dto.request.LoginRequest;
import com.seventest.infrastructure.web.dto.request.PasswordRecoveryRequest;
import com.seventest.infrastructure.web.dto.request.RecoverByNameRequest;
import com.seventest.infrastructure.web.dto.response.ErrorResponse;
import com.seventest.infrastructure.web.dto.response.LoginResponse;
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

@Tag(name = "Autenticación", description = "Login, logout y recuperación de contraseña")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthUseCase authUseCase;

    @Operation(summary = "Iniciar sesión",
            description = "Autentica al usuario y devuelve un token JWT junto con el rol, "
                    + "que el cliente usa para redirigir a la landing correspondiente (HU-07).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Login exitoso",
                    content = @Content(schema = @Schema(implementation = LoginResponse.class))),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Credenciales incorrectas",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Cuenta inactiva",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "423", description = "Cuenta bloqueada por exceso de intentos fallidos",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResult result = authUseCase.login(request.email(), request.password());
        return ResponseEntity.ok(new LoginResponse(result.token(), result.role(), result.userFullName()));
    }

    @Operation(summary = "Cerrar sesión",
            description = "Invalida el token JWT actual. El token queda en lista negra hasta su expiración natural.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Sesión cerrada exitosamente"),
            @ApiResponse(responseCode = "403", description = "Token inválido o ausente")
    })
    @SecurityRequirement(name = "Bearer Auth")
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestHeader("Authorization") String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            authUseCase.logout(authHeader.substring(7));
        }
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Solicitar recuperación de contraseña",
            description = "Registra la solicitud y notifica al equipo de soporte. "
                    + "La respuesta es siempre la misma independientemente de si el email existe, "
                    + "para no revelar información sobre los usuarios registrados.")
    @ApiResponses({
            @ApiResponse(responseCode = "202", description = "Solicitud registrada"),
            @ApiResponse(responseCode = "400", description = "Email con formato inválido",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/password-recovery")
    public ResponseEntity<Void> requestPasswordRecovery(@Valid @RequestBody PasswordRecoveryRequest request) {
        authUseCase.requestPasswordRecovery(request.email());
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/recover-by-name")
    public ResponseEntity<java.util.Map<String, String>> recoverByName(@Valid @RequestBody RecoverByNameRequest request) {
        String email = authUseCase.recoverByName(request.name());
        return ResponseEntity.ok(java.util.Map.of("email", email));
    }
}
