package com.seventest.application.service;

import com.seventest.domain.exception.EmailAlreadyExistsException;
import com.seventest.domain.exception.PasswordPolicyViolationException;
import com.seventest.domain.exception.UserNotFoundException;
import com.seventest.domain.model.*;
import com.seventest.domain.port.out.PasswordPolicyRepository;
import com.seventest.domain.port.out.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock UserRepository userRepository;
    @Mock PasswordPolicyRepository passwordPolicyRepository;
    @Mock PasswordEncoder passwordEncoder;

    @InjectMocks UserService userService;

    private static final PasswordPolicy PERMISSIVE_POLICY = PasswordPolicy.builder()
            .minLength(4).maxLength(100)
            .requireUppercase(false).requireLowercase(false)
            .requireNumbers(false).requireSpecialChars(false)
            .build();

    @BeforeEach
    void setUp() {
        when(passwordPolicyRepository.find()).thenReturn(Optional.of(PERMISSIVE_POLICY));
        when(passwordEncoder.encode(anyString())).thenReturn("hashed");
    }

    private User existingUser(UUID id, String email, UserStatus status) {
        return User.builder()
                .id(id).fullName("Nombre").email(email)
                .role(Role.ALUMNO).status(status)
                .passwordHash("hashed").failedLoginAttempts(0).build();
    }

    // ------------------------------------------------------------------ create
    @Test
    void crearUsuario_conDatosValidos_guardaYRetornaUsuario() {
        when(userRepository.existsByEmail("nuevo@test.com")).thenReturn(false);
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        User result = userService.create("Nombre", "nuevo@test.com", Role.ALUMNO, "pass");

        assertThat(result.getEmail()).isEqualTo("nuevo@test.com");
        assertThat(result.getStatus()).isEqualTo(UserStatus.ACTIVO);
        verify(userRepository).save(any());
    }

    @Test
    void crearUsuario_conEmailDuplicado_lanzaEmailAlreadyExists() {
        when(userRepository.existsByEmail("duplicado@test.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.create("Nombre", "duplicado@test.com", Role.ALUMNO, "pass"))
                .isInstanceOf(EmailAlreadyExistsException.class);

        verify(userRepository, never()).save(any());
    }

    @Test
    void crearUsuario_conContraseñaDemasiadoCorta_lanzaPasswordPolicyViolation() {
        PasswordPolicy strictPolicy = PasswordPolicy.builder()
                .minLength(8).maxLength(100)
                .requireUppercase(false).requireLowercase(false)
                .requireNumbers(false).requireSpecialChars(false)
                .build();
        when(passwordPolicyRepository.find()).thenReturn(Optional.of(strictPolicy));
        when(userRepository.existsByEmail(anyString())).thenReturn(false);

        assertThatThrownBy(() -> userService.create("Nombre", "x@test.com", Role.ALUMNO, "corta"))
                .isInstanceOf(PasswordPolicyViolationException.class);
    }

    // ------------------------------------------------------------------ update
    @Test
    void editarUsuario_actualizaElEmail() {
        UUID id = UUID.randomUUID();
        User user = existingUser(id, "viejo@test.com", UserStatus.ACTIVO);
        when(userRepository.findById(id)).thenReturn(Optional.of(user));
        when(userRepository.existsByEmailAndIdNot("nuevo@test.com", id)).thenReturn(false);
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        User result = userService.update(id, "Nombre", "nuevo@test.com", Role.ALUMNO, null);

        assertThat(result.getEmail()).isEqualTo("nuevo@test.com");
    }

    @Test
    void editarUsuario_conEmailDuplicado_lanzaEmailAlreadyExists() {
        UUID id = UUID.randomUUID();
        User user = existingUser(id, "viejo@test.com", UserStatus.ACTIVO);
        when(userRepository.findById(id)).thenReturn(Optional.of(user));
        when(userRepository.existsByEmailAndIdNot("duplicado@test.com", id)).thenReturn(true);

        assertThatThrownBy(() -> userService.update(id, "Nombre", "duplicado@test.com", Role.ALUMNO, null))
                .isInstanceOf(EmailAlreadyExistsException.class);
    }

    @Test
    void editarUsuario_conNuevaContraseña_laHashea() {
        UUID id = UUID.randomUUID();
        User user = existingUser(id, "test@test.com", UserStatus.ACTIVO);
        when(userRepository.findById(id)).thenReturn(Optional.of(user));
        when(userRepository.existsByEmailAndIdNot(anyString(), eq(id))).thenReturn(false);
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        userService.update(id, "Nombre", "test@test.com", Role.ALUMNO, "nuevaPass");

        verify(passwordEncoder).encode("nuevaPass");
    }

    @Test
    void editarUsuario_conIdInexistente_lanzaUserNotFound() {
        UUID id = UUID.randomUUID();
        when(userRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.update(id, "N", "e@e.com", Role.ALUMNO, null))
                .isInstanceOf(UserNotFoundException.class);
    }

    // --------------------------------------------------------------- deactivate
    @Test
    void desactivarUsuario_cambiaEstadoAInactivo() {
        UUID id = UUID.randomUUID();
        User user = existingUser(id, "test@test.com", UserStatus.ACTIVO);
        when(userRepository.findById(id)).thenReturn(Optional.of(user));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        userService.deactivate(id);

        verify(userRepository).save(argThat(u -> u.getStatus() == UserStatus.INACTIVO));
    }

    // --------------------------------------------------------------- reactivate
    @Test
    void reactivarUsuario_cambiaEstadoAActivo() {
        UUID id = UUID.randomUUID();
        User user = existingUser(id, "test@test.com", UserStatus.INACTIVO);
        when(userRepository.findById(id)).thenReturn(Optional.of(user));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        userService.reactivate(id);

        verify(userRepository).save(argThat(u -> u.getStatus() == UserStatus.ACTIVO));
    }
}
