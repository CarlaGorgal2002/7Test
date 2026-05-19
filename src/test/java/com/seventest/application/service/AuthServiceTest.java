package com.seventest.application.service;

import com.seventest.domain.exception.AccountLockedException;
import com.seventest.domain.exception.InvalidCredentialsException;
import com.seventest.domain.exception.UserInactiveException;
import com.seventest.domain.model.LoginResult;
import com.seventest.domain.model.Role;
import com.seventest.domain.model.User;
import com.seventest.domain.model.UserStatus;
import com.seventest.domain.port.out.EmailPort;
import com.seventest.domain.port.out.UserRepository;
import com.seventest.infrastructure.config.AppProperties;
import com.seventest.infrastructure.security.JwtProvider;
import com.seventest.infrastructure.security.TokenBlacklist;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock UserRepository userRepository;
    @Mock EmailPort emailPort;
    @Mock JwtProvider jwtProvider;
    @Mock TokenBlacklist tokenBlacklist;
    @Mock PasswordEncoder passwordEncoder;
    @Mock AppProperties appProperties;

    @InjectMocks AuthService authService;

    private AppProperties.Security securityConfig;

    private User activeUser(int failedAttempts, Instant lockedUntil) {
        return User.builder()
                .id(UUID.randomUUID())
                .fullName("Test User")
                .email("test@test.com")
                .role(Role.ALUMNO)
                .status(UserStatus.ACTIVO)
                .passwordHash("hashed")
                .failedLoginAttempts(failedAttempts)
                .lockedUntil(lockedUntil)
                .build();
    }

    @BeforeEach
    void setUp() {
        securityConfig = new AppProperties.Security();
        securityConfig.setMaxLoginAttempts(5);
        securityConfig.setLockoutDurationMinutes(15);
        when(appProperties.getSecurity()).thenReturn(securityConfig);
    }

    // ------------------------------------------------------------------ login
    @Test
    void loginExitoso_retornaTokenYRol() {
        User user = activeUser(0, null);
        var page = new com.seventest.domain.model.PageResult<>(List.of(user), 1L, 1, 0);
        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(user));
        when(userRepository.findAll(null, null, UserStatus.ACTIVO, 0, 1000)).thenReturn(page);
        when(passwordEncoder.matches("pass", "hashed")).thenReturn(true);
        when(jwtProvider.generate("test@test.com", "ALUMNO")).thenReturn("jwt-token");

        LoginResult result = authService.login("test@test.com", "pass");

        assertThat(result.token()).isEqualTo("jwt-token");
        assertThat(result.role()).isEqualTo(Role.ALUMNO);
    }

    @Test
    void loginAdmin_soloAceptaSuPropiaContraseña() {
        User admin = User.builder()
                .id(UUID.randomUUID()).fullName("Admin").email("admin@test.com")
                .role(Role.ADMINISTRADOR).status(UserStatus.ACTIVO)
                .passwordHash("adminHash").failedLoginAttempts(0).lockedUntil(null).build();
        when(userRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(admin));
        when(passwordEncoder.matches("adminPass", "adminHash")).thenReturn(true);
        when(jwtProvider.generate("admin@test.com", "ADMINISTRADOR")).thenReturn("admin-token");

        LoginResult result = authService.login("admin@test.com", "adminPass");

        assertThat(result.token()).isEqualTo("admin-token");
        verify(userRepository, never()).findAll(any(), any(), any(), anyInt(), anyInt());
    }

    @Test
    void loginAdmin_conContraseñaDeOtroUsuario_falla() {
        User admin = User.builder()
                .id(UUID.randomUUID()).fullName("Admin").email("admin@test.com")
                .role(Role.ADMINISTRADOR).status(UserStatus.ACTIVO)
                .passwordHash("adminHash").failedLoginAttempts(0).lockedUntil(null).build();
        when(userRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(admin));
        when(passwordEncoder.matches("otroPassword", "adminHash")).thenReturn(false);
        when(appProperties.getSecurity()).thenReturn(securityConfig);

        assertThatThrownBy(() -> authService.login("admin@test.com", "otroPassword"))
                .isInstanceOf(InvalidCredentialsException.class);
        verify(userRepository, never()).findAll(any(), any(), any(), anyInt(), anyInt());
    }

    @Test
    void loginConEmailInexistente_lanzaInvalidCredentials() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login("noexiste@test.com", "pass"))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void loginConUsuarioInactivo_lanzaUserInactive() {
        User inactivo = User.builder()
                .id(UUID.randomUUID()).fullName("Test").email("test@test.com")
                .role(Role.ALUMNO).status(UserStatus.INACTIVO)
                .passwordHash("hashed").failedLoginAttempts(0).build();
        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(inactivo));

        assertThatThrownBy(() -> authService.login("test@test.com", "pass"))
                .isInstanceOf(UserInactiveException.class);
    }

    @Test
    void loginConCuentaBloqueada_lanzaAccountLocked() {
        Instant futuro = Instant.now().plus(10, ChronoUnit.MINUTES);
        User user = activeUser(5, futuro);
        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.login("test@test.com", "pass"))
                .isInstanceOf(AccountLockedException.class);
    }

    @Test
    void loginConContraseñaIncorrecta_incrementaIntentos() {
        User user = activeUser(0, null);
        var page = new com.seventest.domain.model.PageResult<>(List.of(user), 1L, 1, 0);
        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(user));
        when(userRepository.findAll(null, null, UserStatus.ACTIVO, 0, 1000)).thenReturn(page);
        when(passwordEncoder.matches("mala", "hashed")).thenReturn(false);

        assertThatThrownBy(() -> authService.login("test@test.com", "mala"))
                .isInstanceOf(InvalidCredentialsException.class);

        verify(userRepository).save(argThat(u -> u.getFailedLoginAttempts() == 1));
    }

    @Test
    void loginAlLlegarAlLimiteDeIntentos_bloqueaLaCuenta() {
        User user = activeUser(4, null);
        var page = new com.seventest.domain.model.PageResult<>(List.of(user), 1L, 1, 0);
        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(user));
        when(userRepository.findAll(null, null, UserStatus.ACTIVO, 0, 1000)).thenReturn(page);
        when(passwordEncoder.matches("mala", "hashed")).thenReturn(false);

        assertThatThrownBy(() -> authService.login("test@test.com", "mala"))
                .isInstanceOf(InvalidCredentialsException.class);

        verify(userRepository).save(argThat(u ->
                u.getFailedLoginAttempts() == 5 && u.getLockedUntil() != null));
    }

    @Test
    void loginExitoso_reseteaIntentosFallidos() {
        User user = activeUser(3, null);
        var page = new com.seventest.domain.model.PageResult<>(List.of(user), 1L, 1, 0);
        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(user));
        when(userRepository.findAll(null, null, UserStatus.ACTIVO, 0, 1000)).thenReturn(page);
        when(passwordEncoder.matches("pass", "hashed")).thenReturn(true);
        when(jwtProvider.generate(anyString(), anyString())).thenReturn("token");

        authService.login("test@test.com", "pass");

        verify(userRepository).save(argThat(u ->
                u.getFailedLoginAttempts() == 0 && u.getLockedUntil() == null));
    }

    // ------------------------------------------------------------------ logout
    @Test
    void logout_conTokenValido_agregaAlBlacklist() {
        Instant expiry = Instant.now().plus(1, ChronoUnit.HOURS);
        when(jwtProvider.validate("valid-token")).thenReturn(true);
        when(jwtProvider.extractExpiry("valid-token")).thenReturn(expiry);

        authService.logout("valid-token");

        verify(tokenBlacklist).add("valid-token", expiry);
    }

    @Test
    void logout_conTokenInvalido_noAgregaAlBlacklist() {
        when(jwtProvider.validate("bad-token")).thenReturn(false);

        authService.logout("bad-token");

        verifyNoInteractions(tokenBlacklist);
    }

    // --------------------------------------------------------- password recovery
    @Test
    void recuperacionContraseña_conEmailExistente_lllamaAlEmailPort() {
        User user = activeUser(0, null);
        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(user));

        authService.requestPasswordRecovery("test@test.com");

        verify(emailPort).sendPasswordRecoveryNotification("test@test.com", "Test User");
    }

    @Test
    void recuperacionContraseña_conEmailInexistente_noLanzaExcepcion() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        authService.requestPasswordRecovery("noexiste@test.com");

        verifyNoInteractions(emailPort);
    }
}
