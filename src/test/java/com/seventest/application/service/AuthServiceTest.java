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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private EmailPort emailPort;

    @Mock
    private JwtProvider jwtProvider;

    @Mock
    private TokenBlacklist tokenBlacklist;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AppProperties appProperties;

    @InjectMocks
    private AuthService authService;

    private AppProperties.Security securityConfig;

    @BeforeEach
    void setUp() {
        securityConfig = new AppProperties.Security();
        securityConfig.setMaxLoginAttempts(3);
        securityConfig.setLockoutDurationMinutes(15);
    }

    private User createSampleUser(String email, Role role, UserStatus status, int failedAttempts, Instant lockedUntil) {
        return User.builder()
                .id(UUID.randomUUID())
                .fullName("Test User")
                .email(email)
                .role(role)
                .status(status)
                .passwordHash("hashed-password")
                .failedLoginAttempts(failedAttempts)
                .lockedUntil(lockedUntil)
                .build();
    }

    // ========================================== LOGIN ==========================================

    @Test
    void login_withValidCredentialsAndNoFailedAttempts_returnsLoginResultAndDoesNotSaveUser() {
        // Arrange
        User user = createSampleUser("test@example.com", Role.ALUMNO, UserStatus.ACTIVO, 0, null);
        Mockito.when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        Mockito.when(passwordEncoder.matches("password", "hashed-password")).thenReturn(true);
        Mockito.when(jwtProvider.generate("test@example.com", "ALUMNO")).thenReturn("token");

        // Act
        LoginResult result = authService.login("TEST@example.com ", "password");

        // Assert
        Assertions.assertNotNull(result);
        Assertions.assertEquals("token", result.token());
        Assertions.assertEquals(Role.ALUMNO, result.role());
        Assertions.assertEquals("Test User", result.userFullName());
        Mockito.verify(userRepository, Mockito.never()).save(Mockito.any(User.class));
    }

    @Test
    void login_withValidCredentialsAndPreviousFailedAttempts_resetsAttemptsAndSavesUser() {
        // Arrange
        User user = createSampleUser("test@example.com", Role.ALUMNO, UserStatus.ACTIVO, 2, null);
        Mockito.when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        Mockito.when(passwordEncoder.matches("password", "hashed-password")).thenReturn(true);
        Mockito.when(jwtProvider.generate("test@example.com", "ALUMNO")).thenReturn("token");
        Mockito.when(userRepository.save(Mockito.any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        LoginResult result = authService.login("test@example.com", "password");

        // Assert
        Assertions.assertNotNull(result);
        Mockito.verify(userRepository, Mockito.times(1)).save(Mockito.argThat(u -> 
                u.getFailedLoginAttempts() == 0 && u.getLockedUntil() == null
        ));
    }

    @Test
    void login_withNullEmail_handlesNullCorrectlyAndThrowsInvalidCredentialsException() {
        // Arrange
        Mockito.when(userRepository.findByEmail("")).thenReturn(Optional.empty());

        // Act & Assert
        Assertions.assertThrows(
                InvalidCredentialsException.class,
                () -> authService.login(null, "password")
        );
        Mockito.verifyNoInteractions(passwordEncoder, jwtProvider);
    }

    @Test
    void login_userNotFound_throwsInvalidCredentialsException() {
        // Arrange
        Mockito.when(userRepository.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());

        // Act & Assert
        Assertions.assertThrows(
                InvalidCredentialsException.class,
                () -> authService.login("nonexistent@example.com", "password")
        );
    }

    @Test
    void login_userInactive_throwsUserInactiveException() {
        // Arrange
        User user = createSampleUser("test@example.com", Role.ALUMNO, UserStatus.INACTIVO, 0, null);
        Mockito.when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));

        // Act & Assert
        Assertions.assertThrows(
                UserInactiveException.class,
                () -> authService.login("test@example.com", "password")
        );
    }

    @Test
    void login_accountLocked_throwsAccountLockedException() {
        // Arrange
        Instant lockedUntil = Instant.now().plus(10, ChronoUnit.MINUTES);
        User user = createSampleUser("test@example.com", Role.ALUMNO, UserStatus.ACTIVO, 3, lockedUntil);
        Mockito.when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));

        // Act & Assert
        Assertions.assertThrows(
                AccountLockedException.class,
                () -> authService.login("test@example.com", "password")
        );
    }

    @Test
    void login_accountLockExpired_succeedsAndResetsLock() {
        // Arrange
        Instant expiredLock = Instant.now().minus(5, ChronoUnit.MINUTES);
        User user = createSampleUser("test@example.com", Role.ALUMNO, UserStatus.ACTIVO, 3, expiredLock);
        Mockito.when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        Mockito.when(passwordEncoder.matches("password", "hashed-password")).thenReturn(true);
        Mockito.when(jwtProvider.generate("test@example.com", "ALUMNO")).thenReturn("token");
        Mockito.when(userRepository.save(Mockito.any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        LoginResult result = authService.login("test@example.com", "password");

        // Assert
        Assertions.assertNotNull(result);
        Mockito.verify(userRepository, Mockito.times(1)).save(Mockito.argThat(u -> 
                u.getFailedLoginAttempts() == 0 && u.getLockedUntil() == null
        ));
    }

    @Test
    void login_incorrectPasswordAndAttemptsBelowMax_incrementsAttemptsAndSaves() {
        // Arrange
        User user = createSampleUser("test@example.com", Role.ALUMNO, UserStatus.ACTIVO, 0, null);
        Mockito.when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        Mockito.when(passwordEncoder.matches("wrong-pass", "hashed-password")).thenReturn(false);
        Mockito.when(appProperties.getSecurity()).thenReturn(securityConfig);
        Mockito.when(userRepository.save(Mockito.any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act & Assert
        Assertions.assertThrows(
                InvalidCredentialsException.class,
                () -> authService.login("test@example.com", "wrong-pass")
        );
        Mockito.verify(userRepository, Mockito.times(1)).save(Mockito.argThat(u -> 
                u.getFailedLoginAttempts() == 1 && u.getLockedUntil() == null
        ));
    }

    @Test
    void login_incorrectPasswordAndAttemptsReachesMax_locksAccountAndSaves() {
        // Arrange
        User user = createSampleUser("test@example.com", Role.ALUMNO, UserStatus.ACTIVO, 2, null);
        Mockito.when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        Mockito.when(passwordEncoder.matches("wrong-pass", "hashed-password")).thenReturn(false);
        Mockito.when(appProperties.getSecurity()).thenReturn(securityConfig);
        Mockito.when(userRepository.save(Mockito.any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act & Assert
        Assertions.assertThrows(
                InvalidCredentialsException.class,
                () -> authService.login("test@example.com", "wrong-pass")
        );
        Mockito.verify(userRepository, Mockito.times(1)).save(Mockito.argThat(u -> 
                u.getFailedLoginAttempts() == 3 && u.getLockedUntil() != null
        ));
    }

    // ========================================== LOGOUT ==========================================

    @Test
    void logout_withValidToken_addsToBlacklist() {
        // Arrange
        Instant expiry = Instant.now().plus(1, ChronoUnit.HOURS);
        Mockito.when(jwtProvider.validate("valid-token")).thenReturn(true);
        Mockito.when(jwtProvider.extractExpiry("valid-token")).thenReturn(expiry);

        // Act
        authService.logout("valid-token");

        // Assert
        Mockito.verify(tokenBlacklist, Mockito.times(1)).add("valid-token", expiry);
    }

    @Test
    void logout_withInvalidToken_doesNotAddToBlacklist() {
        // Arrange
        Mockito.when(jwtProvider.validate("invalid-token")).thenReturn(false);

        // Act
        authService.logout("invalid-token");

        // Assert
        Mockito.verifyNoInteractions(tokenBlacklist);
    }

    // ========================================== PASSWORD RECOVERY ==========================================

    @Test
    void requestPasswordRecovery_withExistingEmail_sendsEmailNotification() {
        // Arrange
        User user = createSampleUser("test@example.com", Role.ALUMNO, UserStatus.ACTIVO, 0, null);
        Mockito.when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));

        // Act
        authService.requestPasswordRecovery(" TEST@example.com ");

        // Assert
        Mockito.verify(emailPort, Mockito.times(1)).sendPasswordRecoveryNotification("test@example.com", "Test User");
    }

    @Test
    void requestPasswordRecovery_withNonexistentEmail_doesNotSendEmailNotification() {
        // Arrange
        Mockito.when(userRepository.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());

        // Act
        authService.requestPasswordRecovery("nonexistent@example.com");

        // Assert
        Mockito.verifyNoInteractions(emailPort);
    }

    @Test
    void requestPasswordRecovery_withNullEmail_handlesNullAndDoesNotSendEmailNotification() {
        // Arrange
        Mockito.when(userRepository.findByEmail("")).thenReturn(Optional.empty());

        // Act
        authService.requestPasswordRecovery(null);

        // Assert
        Mockito.verifyNoInteractions(emailPort);
    }

    @Test
    void recoverByName_withExistingFullName_returnsUserEmail() {
        // Arrange
        User user = createSampleUser("test@example.com", Role.ALUMNO, UserStatus.ACTIVO, 0, null);
        Mockito.when(userRepository.findByFullName("Test User")).thenReturn(Optional.of(user));

        // Act
        String result = authService.recoverByName("Test User");

        // Assert
        Assertions.assertEquals("test@example.com", result);
        Mockito.verify(userRepository, Mockito.times(1)).findByFullName("Test User");
    }

    @Test
    void recoverByName_withNonexistentFullName_returnsEmptyString() {
        // Arrange
        Mockito.when(userRepository.findByFullName("Nonexistent")).thenReturn(Optional.empty());

        // Act
        String result = authService.recoverByName("Nonexistent");

        // Assert
        Assertions.assertEquals("", result);
        Mockito.verify(userRepository, Mockito.times(1)).findByFullName("Nonexistent");
    }
}
