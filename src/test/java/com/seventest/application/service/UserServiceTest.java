package com.seventest.application.service;

import com.seventest.domain.exception.EmailAlreadyExistsException;
import com.seventest.domain.exception.PasswordPolicyViolationException;
import com.seventest.domain.exception.UserNotFoundException;
import com.seventest.domain.model.PageResult;
import com.seventest.domain.model.PasswordPolicy;
import com.seventest.domain.model.Role;
import com.seventest.domain.model.User;
import com.seventest.domain.model.UserStatus;
import com.seventest.domain.port.out.PasswordPolicyRepository;
import com.seventest.domain.port.out.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock UserRepository userRepository;
    @Mock PasswordPolicyRepository passwordPolicyRepository;
    @Mock PasswordEncoder passwordEncoder;

    @InjectMocks UserService userService;

    private static final PasswordPolicy PERMISSIVE_POLICY = PasswordPolicy.builder()
            .minLength(4)
            .maxLength(100)
            .requireUppercase(false)
            .requireLowercase(false)
            .requireNumbers(false)
            .requireSpecialChars(false)
            .build();

    private User existingUser(UUID id, String email, UserStatus status) {
        return User.builder()
                .id(id)
                .fullName("Nombre")
                .email(email)
                .role(Role.ALUMNO)
                .status(status)
                .passwordHash("old-hash")
                .failedLoginAttempts(0)
                .lockedUntil(null)
                .build();
    }

    private void stubPolicy(PasswordPolicy policy) {
        when(passwordPolicyRepository.find()).thenReturn(Optional.of(policy));
    }

    private void stubEncoder(String encodedValue) {
        when(passwordEncoder.encode(anyString())).thenReturn(encodedValue);
    }

    @Test
    void crearUsuario_conDatosValidos_guardaUsuarioActivoConPasswordHasheada() {
        when(userRepository.existsByEmail("nuevo@test.com")).thenReturn(false);
        stubPolicy(PERMISSIVE_POLICY);
        stubEncoder("hashed-pass");
        when(userRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        User result = userService.create("Nombre", "nuevo@test.com", Role.ALUMNO, "pass");

        assertThat(result.getId()).isNotNull();
        assertThat(result.getEmail()).isEqualTo("nuevo@test.com");
        assertThat(result.getStatus()).isEqualTo(UserStatus.ACTIVO);
        assertThat(result.getPasswordHash()).isEqualTo("hashed-pass");
        verify(userRepository).save(argThat(u ->
                u.getFailedLoginAttempts() == 0 && u.getLockedUntil() == null));
    }

    @Test
    void crearUsuario_conEmailDuplicado_lanzaEmailAlreadyExists() {
        when(userRepository.existsByEmail("duplicado@test.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.create("Nombre", "duplicado@test.com", Role.ALUMNO, "pass"))
                .isInstanceOf(EmailAlreadyExistsException.class);

        verify(userRepository, never()).save(any());
        verifyNoInteractions(passwordPolicyRepository, passwordEncoder);
    }

    @Test
    void crearUsuario_sinPoliticaPersistida_usaPoliticaDefault() {
        when(userRepository.existsByEmail("nuevo@test.com")).thenReturn(false);
        when(passwordPolicyRepository.find()).thenReturn(Optional.empty());
        stubEncoder("hashed-pass");
        when(userRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        User result = userService.create("Nombre", "nuevo@test.com", Role.ALUMNO, "1234");

        assertThat(result.getPasswordHash()).isEqualTo("hashed-pass");
    }

    @Test
    void crearUsuario_conPasswordDemasiadoCorta_lanzaPasswordPolicyViolation() {
        PasswordPolicy strictPolicy = PasswordPolicy.builder()
                .minLength(8)
                .maxLength(100)
                .requireUppercase(false)
                .requireLowercase(false)
                .requireNumbers(false)
                .requireSpecialChars(false)
                .build();
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        stubPolicy(strictPolicy);

        assertThatThrownBy(() -> userService.create("Nombre", "x@test.com", Role.ALUMNO, "corta"))
                .isInstanceOf(PasswordPolicyViolationException.class)
                .hasMessageContaining("8");

        verify(userRepository, never()).save(any());
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void crearUsuario_conPasswordDemasiadoLarga_lanzaPasswordPolicyViolation() {
        PasswordPolicy strictPolicy = PasswordPolicy.builder()
                .minLength(4)
                .maxLength(6)
                .requireUppercase(false)
                .requireLowercase(false)
                .requireNumbers(false)
                .requireSpecialChars(false)
                .build();
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        stubPolicy(strictPolicy);

        assertThatThrownBy(() -> userService.create("Nombre", "x@test.com", Role.ALUMNO, "1234567"))
                .isInstanceOf(PasswordPolicyViolationException.class)
                .hasMessageContaining("6");
    }

    @Test
    void crearUsuario_conPoliticaCompleta_validaMayusculaMinusculaNumeroYEspecial() {
        PasswordPolicy strictPolicy = PasswordPolicy.builder()
                .minLength(8)
                .maxLength(20)
                .requireUppercase(true)
                .requireLowercase(true)
                .requireNumbers(true)
                .requireSpecialChars(true)
                .build();
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        stubPolicy(strictPolicy);
        stubEncoder("hashed-pass");
        when(userRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        User result = userService.create("Nombre", "x@test.com", Role.ALUMNO, "Pass123!");

        assertThat(result.getPasswordHash()).isEqualTo("hashed-pass");
    }

    @Test
    void crearUsuario_sinMayusculaCuandoEsRequerida_lanzaPasswordPolicyViolation() {
        PasswordPolicy strictPolicy = PasswordPolicy.builder()
                .minLength(4)
                .maxLength(20)
                .requireUppercase(true)
                .requireLowercase(false)
                .requireNumbers(false)
                .requireSpecialChars(false)
                .build();
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        stubPolicy(strictPolicy);

        assertThatThrownBy(() -> userService.create("Nombre", "x@test.com", Role.ALUMNO, "pass"))
                .isInstanceOf(PasswordPolicyViolationException.class)
                .hasMessageContaining("may");
    }

    @Test
    void crearUsuario_sinNumeroCuandoEsRequerido_lanzaPasswordPolicyViolation() {
        PasswordPolicy strictPolicy = PasswordPolicy.builder()
                .minLength(4)
                .maxLength(20)
                .requireUppercase(false)
                .requireLowercase(false)
                .requireNumbers(true)
                .requireSpecialChars(false)
                .build();
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        stubPolicy(strictPolicy);

        assertThatThrownBy(() -> userService.create("Nombre", "x@test.com", Role.ALUMNO, "pass"))
                .isInstanceOf(PasswordPolicyViolationException.class)
                .hasMessageContaining("n");
    }

    @Test
    void editarUsuario_actualizaDatosSinCambiarPasswordSiNewPasswordEsNull() {
        UUID id = UUID.randomUUID();
        User user = existingUser(id, "viejo@test.com", UserStatus.ACTIVO);
        when(userRepository.findById(id)).thenReturn(Optional.of(user));
        when(userRepository.existsByEmailAndIdNot("nuevo@test.com", id)).thenReturn(false);
        when(userRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        User result = userService.update(id, "Nombre Nuevo", "nuevo@test.com", Role.PROFESOR, null);

        assertThat(result.getFullName()).isEqualTo("Nombre Nuevo");
        assertThat(result.getEmail()).isEqualTo("nuevo@test.com");
        assertThat(result.getRole()).isEqualTo(Role.PROFESOR);
        assertThat(result.getPasswordHash()).isEqualTo("old-hash");
        verifyNoInteractions(passwordPolicyRepository, passwordEncoder);
    }

    @Test
    void editarUsuario_conEmailDuplicado_lanzaEmailAlreadyExists() {
        UUID id = UUID.randomUUID();
        User user = existingUser(id, "viejo@test.com", UserStatus.ACTIVO);
        when(userRepository.findById(id)).thenReturn(Optional.of(user));
        when(userRepository.existsByEmailAndIdNot("duplicado@test.com", id)).thenReturn(true);

        assertThatThrownBy(() -> userService.update(id, "Nombre", "duplicado@test.com", Role.ALUMNO, null))
                .isInstanceOf(EmailAlreadyExistsException.class);

        verify(userRepository, never()).save(any());
    }

    @Test
    void editarUsuario_conNuevaPassword_laHashea() {
        UUID id = UUID.randomUUID();
        User user = existingUser(id, "test@test.com", UserStatus.ACTIVO);
        when(userRepository.findById(id)).thenReturn(Optional.of(user));
        stubPolicy(PERMISSIVE_POLICY);
        stubEncoder("new-hash");
        when(userRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        User result = userService.update(id, "Nombre", "test@test.com", Role.ALUMNO, "nuevaPass");

        assertThat(result.getPasswordHash()).isEqualTo("new-hash");
        verify(passwordEncoder).encode("nuevaPass");
    }

    @Test
    void editarUsuario_conNewPasswordEnBlanco_mantieneHashAnterior() {
        UUID id = UUID.randomUUID();
        User user = existingUser(id, "test@test.com", UserStatus.ACTIVO);
        when(userRepository.findById(id)).thenReturn(Optional.of(user));
        when(userRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        User result = userService.update(id, "Nombre", "test@test.com", Role.ALUMNO, "   ");

        assertThat(result.getPasswordHash()).isEqualTo("old-hash");
        verifyNoInteractions(passwordPolicyRepository, passwordEncoder);
    }

    @Test
    void editarUsuario_conIdInexistente_lanzaUserNotFound() {
        UUID id = UUID.randomUUID();
        when(userRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.update(id, "N", "e@e.com", Role.ALUMNO, null))
                .isInstanceOf(UserNotFoundException.class);

        verify(userRepository, never()).save(any());
    }

    @Test
    void desactivarUsuario_cambiaEstadoAInactivo() {
        UUID id = UUID.randomUUID();
        User user = existingUser(id, "test@test.com", UserStatus.ACTIVO);
        when(userRepository.findById(id)).thenReturn(Optional.of(user));

        userService.deactivate(id);

        verify(userRepository).save(argThat(u -> u.getStatus() == UserStatus.INACTIVO));
    }

    @Test
    void reactivarUsuario_cambiaEstadoAActivo() {
        UUID id = UUID.randomUUID();
        User user = existingUser(id, "test@test.com", UserStatus.INACTIVO);
        when(userRepository.findById(id)).thenReturn(Optional.of(user));

        userService.reactivate(id);

        verify(userRepository).save(argThat(u -> u.getStatus() == UserStatus.ACTIVO));
    }

    @Test
    void findById_conUsuarioExistente_loRetorna() {
        UUID id = UUID.randomUUID();
        User user = existingUser(id, "test@test.com", UserStatus.ACTIVO);
        when(userRepository.findById(id)).thenReturn(Optional.of(user));

        User result = userService.findById(id);

        assertThat(result).isSameAs(user);
    }

    @Test
    void list_delegaFiltrosYPaginadoAlRepositorio() {
        PageResult<User> page = new PageResult<>(List.of(), 0, 0, 1);
        when(userRepository.findAll("ana", Role.ALUMNO, UserStatus.ACTIVO, 1, 20)).thenReturn(page);

        PageResult<User> result = userService.list("ana", Role.ALUMNO, UserStatus.ACTIVO, 1, 20);

        assertThat(result).isSameAs(page);
    }
}
