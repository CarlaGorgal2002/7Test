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
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

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

    @BeforeEach
    void setUp() {
        securityConfig = new AppProperties.Security();
        securityConfig.setMaxLoginAttempts(5);
        securityConfig.setLockoutDurationMinutes(15);
    }

    private User activeUser(String email, Role role, String passwordHash, int failedAttempts, Instant lockedUntil) {
        return User.builder()
                .id(UUID.randomUUID())
                .fullName("Test User")
                .email(email)
                .role(role)
                .status(UserStatus.ACTIVO)
                .passwordHash(passwordHash)
                .failedLoginAttempts(failedAttempts)
                .lockedUntil(lockedUntil)
                .build();
    }

    private User inactiveUser(String email) {
        return activeUser(email, Role.ALUMNO, "hashed", 0, null)
                .toBuilder()
                .status(UserStatus.INACTIVO)
                .build();
    }

    private void stubSecurityProperties() {
        when(appProperties.getSecurity()).thenReturn(securityConfig);
    }

    @Test
    void loginNoAdmin_conCredencialesValidas_retornaTokenRolYNombre() {
        User user = activeUser("alumno@test.com", Role.ALUMNO, "hashA", 0, null);
        when(userRepository.findByEmail("alumno@test.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("passA", "hashA")).thenReturn(true);
        when(jwtProvider.generate("alumno@test.com", "ALUMNO")).thenReturn("jwt-token");

        LoginResult result = authService.login("alumno@test.com", "passA");

        assertThat(result.token()).isEqualTo("jwt-token");
        assertThat(result.role()).isEqualTo(Role.ALUMNO);
        assertThat(result.userFullName()).isEqualTo("Test User");
    }

    @Test
    void loginNoAdmin_conPasswordDeOtroUsuario_rechazaCredenciales() {
        User userA = activeUser("a@test.com", Role.ALUMNO, "hashA", 0, null);
        when(userRepository.findByEmail("a@test.com")).thenReturn(Optional.of(userA));
        when(passwordEncoder.matches("passB", "hashA")).thenReturn(false);
        stubSecurityProperties();

        assertThatThrownBy(() -> authService.login("a@test.com", "passB"))
                .isInstanceOf(InvalidCredentialsException.class);

        verify(userRepository, never()).findAll(null, null, UserStatus.ACTIVO, 0, 1000);
        verify(userRepository).save(argThat(u -> u.getFailedLoginAttempts() == 1));
    }

    @Test
    void loginAdmin_soloAceptaSuPropiaPassword() {
        User admin = activeUser("admin@test.com", Role.ADMINISTRADOR, "adminHash", 0, null);
        when(userRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(admin));
        when(passwordEncoder.matches("adminPass", "adminHash")).thenReturn(true);
        when(jwtProvider.generate("admin@test.com", "ADMINISTRADOR")).thenReturn("admin-token");

        LoginResult result = authService.login("admin@test.com", "adminPass");

        assertThat(result.token()).isEqualTo("admin-token");
        verify(userRepository, never()).findAll(null, null, UserStatus.ACTIVO, 0, 1000);
    }

    @Test
    void loginAdmin_conPasswordDeOtroUsuario_falla() {
        User admin = activeUser("admin@test.com", Role.ADMINISTRADOR, "adminHash", 0, null);
        when(userRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(admin));
        when(passwordEncoder.matches("otroPassword", "adminHash")).thenReturn(false);
        stubSecurityProperties();

        assertThatThrownBy(() -> authService.login("admin@test.com", "otroPassword"))
                .isInstanceOf(InvalidCredentialsException.class);

        verify(userRepository, never()).findAll(null, null, UserStatus.ACTIVO, 0, 1000);
        verify(userRepository).save(argThat(u -> u.getFailedLoginAttempts() == 1));
    }

    @Test
    void loginConEmailInexistente_lanzaInvalidCredentials() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login("noexiste@test.com", "pass"))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void loginConUsuarioInactivo_lanzaUserInactive() {
        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(inactiveUser("test@test.com")));

        assertThatThrownBy(() -> authService.login("test@test.com", "pass"))
                .isInstanceOf(UserInactiveException.class);
    }

    @Test
    void loginConCuentaBloqueada_lanzaAccountLocked() {
        Instant lockedUntil = Instant.now().plus(10, ChronoUnit.MINUTES);
        User user = activeUser("test@test.com", Role.ALUMNO, "hashed", 5, lockedUntil);
        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.login("test@test.com", "pass"))
                .isInstanceOf(AccountLockedException.class);
    }

    @Test
    void loginConPasswordIncorrecta_incrementaIntentos() {
        User user = activeUser("test@test.com", Role.ALUMNO, "hashed", 0, null);
        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("mala", "hashed")).thenReturn(false);
        stubSecurityProperties();

        assertThatThrownBy(() -> authService.login("test@test.com", "mala"))
                .isInstanceOf(InvalidCredentialsException.class);

        verify(userRepository).save(argThat(u -> u.getFailedLoginAttempts() == 1));
    }

    @Test
    void loginAlLlegarAlLimiteDeIntentos_bloqueaLaCuenta() {
        User user = activeUser("test@test.com", Role.ALUMNO, "hashed", 4, null);
        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("mala", "hashed")).thenReturn(false);
        stubSecurityProperties();

        assertThatThrownBy(() -> authService.login("test@test.com", "mala"))
                .isInstanceOf(InvalidCredentialsException.class);

        verify(userRepository).save(argThat(u ->
                u.getFailedLoginAttempts() == 5 && u.getLockedUntil() != null));
    }

    @Test
    void loginExitoso_reseteaIntentosFallidosYBloqueoPrevioVencido() {
        Instant expiredLock = Instant.now().minus(1, ChronoUnit.MINUTES);
        User user = activeUser("test@test.com", Role.ALUMNO, "hashed", 3, expiredLock);
        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("pass", "hashed")).thenReturn(true);
        when(jwtProvider.generate("test@test.com", "ALUMNO")).thenReturn("token");

        authService.login("test@test.com", "pass");

        verify(userRepository).save(argThat(u ->
                u.getFailedLoginAttempts() == 0 && u.getLockedUntil() == null));
    }

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

    @Test
    void recuperacionPassword_conEmailExistente_llamaAlEmailPort() {
        User user = activeUser("test@test.com", Role.ALUMNO, "hashed", 0, null);
        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(user));

        authService.requestPasswordRecovery("test@test.com");

        verify(emailPort).sendPasswordRecoveryNotification("test@test.com", "Test User");
    }

    @Test
    void recuperacionPassword_conEmailInexistente_noLanzaExcepcion() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        authService.requestPasswordRecovery("noexiste@test.com");

        verifyNoInteractions(emailPort);
    }

    @Test
    void login_normalizaEmailAntesDeBuscar() {
        User user = activeUser("test@test.com", Role.ALUMNO, "hashed", 0, null);
        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("pass", "hashed")).thenReturn(true);
        when(jwtProvider.generate("test@test.com", "ALUMNO")).thenReturn("token");

        LoginResult result = authService.login("  TEST@test.com  ", "pass");

        assertThat(result.token()).isEqualTo("token");
    }
}
