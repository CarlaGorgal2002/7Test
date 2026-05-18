package com.seventest.infrastructure.web.controller;

import com.seventest.domain.model.Role;
import com.seventest.domain.model.User;
import com.seventest.domain.model.UserStatus;
import com.seventest.domain.port.in.UserManagementUseCase;
import com.seventest.infrastructure.web.dto.request.CreateUserRequest;
import com.seventest.infrastructure.web.dto.request.UpdateUserRequest;
import com.seventest.infrastructure.web.dto.response.ErrorResponse;
import com.seventest.infrastructure.web.dto.response.UserPageResponse;
import com.seventest.infrastructure.web.dto.response.UserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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

import java.net.URI;
import java.util.UUID;

@Tag(name = "Usuarios", description = "Alta, consulta, edición y desactivación de usuarios. Solo ADMINISTRADOR.")
@SecurityRequirement(name = "Bearer Auth")
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserManagementUseCase userManagementUseCase;

    @Operation(summary = "Crear usuario",
            description = "Crea un nuevo usuario con estado ACTIVO. "
                    + "La contraseña inicial debe cumplir la política de contraseñas vigente.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Usuario creado exitosamente",
                    content = @Content(schema = @Schema(implementation = UserResponse.class))),
            @ApiResponse(responseCode = "400", description = "Datos inválidos o contraseña viola la política",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Sin autorización o rol insuficiente"),
            @ApiResponse(responseCode = "409", description = "El email ya está registrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping
    public ResponseEntity<UserResponse> create(@Valid @RequestBody CreateUserRequest request) {
        User user = userManagementUseCase.create(
                request.fullName(), request.email(), request.role(), request.initialPassword());
        return ResponseEntity
                .created(URI.create("/api/users/" + user.getId()))
                .body(toResponse(user));
    }

    @Operation(summary = "Listar usuarios",
            description = "Devuelve la lista paginada de usuarios. "
                    + "Soporta búsqueda por nombre o email y filtros por rol y estado.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Listado obtenido exitosamente",
                    content = @Content(schema = @Schema(implementation = UserPageResponse.class))),
            @ApiResponse(responseCode = "403", description = "Sin autorización o rol insuficiente")
    })
    @GetMapping
    public ResponseEntity<UserPageResponse> list(
            @Parameter(description = "Texto a buscar en nombre o email (parcial, case-insensitive)")
            @RequestParam(required = false) String search,
            @Parameter(description = "Filtrar por rol")
            @RequestParam(required = false) Role role,
            @Parameter(description = "Filtrar por estado")
            @RequestParam(required = false) UserStatus status,
            @Parameter(description = "Número de página (base 0)")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Cantidad de resultados por página")
            @RequestParam(defaultValue = "20") int size) {
        var result = userManagementUseCase.list(search, role, status, page, size);
        var content = result.content().stream().map(this::toResponse).toList();
        return ResponseEntity.ok(new UserPageResponse(content, result.totalElements(), result.totalPages(), result.page()));
    }

    @Operation(summary = "Obtener usuario por ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Usuario encontrado",
                    content = @Content(schema = @Schema(implementation = UserResponse.class))),
            @ApiResponse(responseCode = "403", description = "Sin autorización o rol insuficiente"),
            @ApiResponse(responseCode = "404", description = "Usuario no encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(toResponse(userManagementUseCase.findById(id)));
    }

    @Operation(summary = "Editar usuario",
            description = "Actualiza nombre, email y/o rol. "
                    + "Si se incluye `newPassword`, la contraseña es reemplazada — debe cumplir la política vigente. "
                    + "Enviar `null` en `newPassword` para no modificarla.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Usuario actualizado",
                    content = @Content(schema = @Schema(implementation = UserResponse.class))),
            @ApiResponse(responseCode = "400", description = "Datos inválidos o contraseña viola la política",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Sin autorización o rol insuficiente"),
            @ApiResponse(responseCode = "404", description = "Usuario no encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "El email ya está en uso por otro usuario",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PutMapping("/{id}")
    public ResponseEntity<UserResponse> update(@PathVariable UUID id,
                                               @Valid @RequestBody UpdateUserRequest request) {
        User user = userManagementUseCase.update(
                id, request.fullName(), request.email(), request.role(), request.newPassword());
        return ResponseEntity.ok(toResponse(user));
    }

    @Operation(summary = "Desactivar usuario",
            description = "Cambia el estado del usuario a INACTIVO (soft delete). "
                    + "El usuario no podrá iniciar sesión pero sus datos se conservan.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Usuario desactivado"),
            @ApiResponse(responseCode = "403", description = "Sin autorización o rol insuficiente"),
            @ApiResponse(responseCode = "404", description = "Usuario no encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<Void> deactivate(@PathVariable UUID id) {
        userManagementUseCase.deactivate(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Reactivar usuario",
            description = "Cambia el estado del usuario de INACTIVO a ACTIVO. "
                    + "El usuario recupera el acceso a la plataforma.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Usuario reactivado"),
            @ApiResponse(responseCode = "403", description = "Sin autorización o rol insuficiente"),
            @ApiResponse(responseCode = "404", description = "Usuario no encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PatchMapping("/{id}/reactivate")
    public ResponseEntity<Void> reactivate(@PathVariable UUID id) {
        userManagementUseCase.reactivate(id);
        return ResponseEntity.noContent().build();
    }

    private UserResponse toResponse(User user) {
        return new UserResponse(user.getId(), user.getFullName(), user.getEmail(),
                user.getRole(), user.getStatus());
    }
}
