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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordPolicyRepository passwordPolicyRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private static final PasswordPolicy PERMISSIVE_POLICY = PasswordPolicy.builder()
            .minLength(4)
            .maxLength(100)
            .requireUppercase(false)
            .requireLowercase(false)
            .requireNumbers(false)
            .requireSpecialChars(false)
            .build();

    private User createSampleUser(UUID id, String email, UserStatus status) {
        return User.builder()
                .id(id)
                .fullName("John Doe")
                .email(email)
                .role(Role.ALUMNO)
                .status(status)
                .passwordHash("hashed-password")
                .failedLoginAttempts(0)
                .lockedUntil(null)
                .build();
    }

    // ========================================== CREATE ==========================================

    @Test
    void create_withValidDataAndDefaultPolicy_savesAndReturnsUser() {
        // Arrange
        Mockito.when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
        Mockito.when(passwordPolicyRepository.find()).thenReturn(Optional.empty());
        Mockito.when(passwordEncoder.encode("Password123!")).thenReturn("encoded-pass");
        Mockito.when(userRepository.save(Mockito.any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        User result = userService.create("John Doe", "TEST@example.com ", Role.ALUMNO, "Password123!");

        // Assert
        Assertions.assertNotNull(result);
        Assertions.assertEquals("test@example.com", result.getEmail());
        Assertions.assertEquals("John Doe", result.getFullName());
        Assertions.assertEquals(UserStatus.ACTIVO, result.getStatus());
        Assertions.assertEquals("encoded-pass", result.getPasswordHash());
        Mockito.verify(userRepository, Mockito.times(1)).save(Mockito.any(User.class));
    }

    @Test
    void create_withNullFullNameAndNullEmail_handlesNullParametersCorrectly() {
        // Arrange
        Mockito.when(userRepository.existsByEmail("")).thenReturn(false);
        Mockito.when(passwordPolicyRepository.find()).thenReturn(Optional.of(PERMISSIVE_POLICY));
        Mockito.when(passwordEncoder.encode("pass")).thenReturn("encoded-pass");
        Mockito.when(userRepository.save(Mockito.any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        User result = userService.create(null, null, Role.ALUMNO, "pass");

        // Assert
        Assertions.assertNotNull(result);
        Assertions.assertEquals("", result.getEmail());
        Assertions.assertEquals("", result.getFullName());
        Mockito.verify(userRepository, Mockito.times(1)).save(Mockito.any(User.class));
    }

    @Test
    void create_withAlreadyExistingEmail_throwsEmailAlreadyExistsException() {
        // Arrange
        Mockito.when(userRepository.existsByEmail("test@example.com")).thenReturn(true);

        // Act & Assert
        EmailAlreadyExistsException exception = Assertions.assertThrows(
                EmailAlreadyExistsException.class,
                () -> userService.create("John", "test@example.com", Role.ALUMNO, "pass")
        );
        Assertions.assertEquals("El email test@example.com ya está registrado", exception.getMessage());
        Mockito.verify(userRepository, Mockito.never()).save(Mockito.any(User.class));
    }

    @Test
    void create_withPasswordTooShort_throwsPasswordPolicyViolationException() {
        // Arrange
        PasswordPolicy policy = PasswordPolicy.builder().minLength(8).maxLength(20).build();
        Mockito.when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
        Mockito.when(passwordPolicyRepository.find()).thenReturn(Optional.of(policy));

        // Act & Assert
        PasswordPolicyViolationException exception = Assertions.assertThrows(
                PasswordPolicyViolationException.class,
                () -> userService.create("John", "test@example.com", Role.ALUMNO, "short")
        );
        Assertions.assertTrue(exception.getMessage().contains("debe tener al menos 8 caracteres"));
    }

    @Test
    void create_withPasswordTooLong_throwsPasswordPolicyViolationException() {
        // Arrange
        PasswordPolicy policy = PasswordPolicy.builder().minLength(4).maxLength(6).build();
        Mockito.when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
        Mockito.when(passwordPolicyRepository.find()).thenReturn(Optional.of(policy));

        // Act & Assert
        PasswordPolicyViolationException exception = Assertions.assertThrows(
                PasswordPolicyViolationException.class,
                () -> userService.create("John", "test@example.com", Role.ALUMNO, "toolong")
        );
        Assertions.assertTrue(exception.getMessage().contains("no puede superar 6 caracteres"));
    }

    @Test
    void create_missingUppercaseWhenRequired_throwsPasswordPolicyViolationException() {
        // Arrange
        PasswordPolicy policy = PasswordPolicy.builder().minLength(4).maxLength(20).requireUppercase(true).build();
        Mockito.when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
        Mockito.when(passwordPolicyRepository.find()).thenReturn(Optional.of(policy));

        // Act & Assert
        PasswordPolicyViolationException exception = Assertions.assertThrows(
                PasswordPolicyViolationException.class,
                () -> userService.create("John", "test@example.com", Role.ALUMNO, "no-upper")
        );
        Assertions.assertTrue(exception.getMessage().contains("debe contener al menos una mayúscula"));
    }

    @Test
    void create_missingLowercaseWhenRequired_throwsPasswordPolicyViolationException() {
        // Arrange
        PasswordPolicy policy = PasswordPolicy.builder().minLength(4).maxLength(20).requireLowercase(true).build();
        Mockito.when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
        Mockito.when(passwordPolicyRepository.find()).thenReturn(Optional.of(policy));

        // Act & Assert
        PasswordPolicyViolationException exception = Assertions.assertThrows(
                PasswordPolicyViolationException.class,
                () -> userService.create("John", "test@example.com", Role.ALUMNO, "NO-LOWER")
        );
        Assertions.assertTrue(exception.getMessage().contains("debe contener al menos una minúscula"));
    }

    @Test
    void create_missingNumbersWhenRequired_throwsPasswordPolicyViolationException() {
        // Arrange
        PasswordPolicy policy = PasswordPolicy.builder().minLength(4).maxLength(20).requireNumbers(true).build();
        Mockito.when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
        Mockito.when(passwordPolicyRepository.find()).thenReturn(Optional.of(policy));

        // Act & Assert
        PasswordPolicyViolationException exception = Assertions.assertThrows(
                PasswordPolicyViolationException.class,
                () -> userService.create("John", "test@example.com", Role.ALUMNO, "noNumbers")
        );
        Assertions.assertTrue(exception.getMessage().contains("debe contener al menos un número"));
    }

    @Test
    void create_missingSpecialCharsWhenRequired_throwsPasswordPolicyViolationException() {
        // Arrange
        PasswordPolicy policy = PasswordPolicy.builder().minLength(4).maxLength(20).requireSpecialChars(true).build();
        Mockito.when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
        Mockito.when(passwordPolicyRepository.find()).thenReturn(Optional.of(policy));

        // Act & Assert
        PasswordPolicyViolationException exception = Assertions.assertThrows(
                PasswordPolicyViolationException.class,
                () -> userService.create("John", "test@example.com", Role.ALUMNO, "alphanumeric123")
        );
        Assertions.assertTrue(exception.getMessage().contains("debe contener al menos un carácter especial"));
    }

    // ========================================== UPDATE ==========================================

    @Test
    void update_userDoesNotExist_throwsUserNotFoundException() {
        // Arrange
        UUID id = UUID.randomUUID();
        Mockito.when(userRepository.findById(id)).thenReturn(Optional.empty());

        // Act & Assert
        UserNotFoundException exception = Assertions.assertThrows(
                UserNotFoundException.class,
                () -> userService.update(id, "John", "test@test.com", Role.ALUMNO, null)
        );
        Assertions.assertEquals("Usuario no encontrado con ID: " + id, exception.getMessage());
    }

    @Test
    void update_withNewValidEmail_updatesCorrectly() {
        // Arrange
        UUID id = UUID.randomUUID();
        User existingUser = createSampleUser(id, "old@example.com", UserStatus.ACTIVO);
        Mockito.when(userRepository.findById(id)).thenReturn(Optional.of(existingUser));
        Mockito.when(userRepository.existsByEmailAndIdNot("new@example.com", id)).thenReturn(false);
        Mockito.when(userRepository.save(Mockito.any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        User result = userService.update(id, "John New", "new@example.com", Role.PROFESOR, null);

        // Assert
        Assertions.assertNotNull(result);
        Assertions.assertEquals("new@example.com", result.getEmail());
        Assertions.assertEquals("John New", result.getFullName());
        Assertions.assertEquals(Role.PROFESOR, result.getRole());
        Assertions.assertEquals("hashed-password", result.getPasswordHash()); // unchanged
        Mockito.verify(userRepository, Mockito.times(1)).save(Mockito.any(User.class));
    }

    @Test
    void update_withDuplicateEmail_throwsEmailAlreadyExistsException() {
        // Arrange
        UUID id = UUID.randomUUID();
        User existingUser = createSampleUser(id, "old@example.com", UserStatus.ACTIVO);
        Mockito.when(userRepository.findById(id)).thenReturn(Optional.of(existingUser));
        Mockito.when(userRepository.existsByEmailAndIdNot("duplicate@example.com", id)).thenReturn(true);

        // Act & Assert
        EmailAlreadyExistsException exception = Assertions.assertThrows(
                EmailAlreadyExistsException.class,
                () -> userService.update(id, "John", "duplicate@example.com", Role.ALUMNO, null)
        );
        Assertions.assertEquals("El email duplicate@example.com ya está registrado", exception.getMessage());
        Mockito.verify(userRepository, Mockito.never()).save(Mockito.any(User.class));
    }

    @Test
    void update_withNewPassword_hashesAndUpdatesPassword() {
        // Arrange
        UUID id = UUID.randomUUID();
        User existingUser = createSampleUser(id, "test@example.com", UserStatus.ACTIVO);
        Mockito.when(userRepository.findById(id)).thenReturn(Optional.of(existingUser));
        Mockito.when(passwordPolicyRepository.find()).thenReturn(Optional.of(PERMISSIVE_POLICY));
        Mockito.when(passwordEncoder.encode("newPassword123")).thenReturn("new-hashed-password");
        Mockito.when(userRepository.save(Mockito.any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        User result = userService.update(id, "John", "test@example.com", Role.ALUMNO, "newPassword123");

        // Assert
        Assertions.assertNotNull(result);
        Assertions.assertEquals("new-hashed-password", result.getPasswordHash());
        Mockito.verify(passwordEncoder, Mockito.times(1)).encode("newPassword123");
    }

    @Test
    void update_withEmptyPassword_keepsOldPassword() {
        // Arrange
        UUID id = UUID.randomUUID();
        User existingUser = createSampleUser(id, "test@example.com", UserStatus.ACTIVO);
        Mockito.when(userRepository.findById(id)).thenReturn(Optional.of(existingUser));
        Mockito.when(userRepository.save(Mockito.any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        User result = userService.update(id, "John", "test@example.com", Role.ALUMNO, "   ");

        // Assert
        Assertions.assertNotNull(result);
        Assertions.assertEquals("hashed-password", result.getPasswordHash());
        Mockito.verifyNoInteractions(passwordEncoder, passwordPolicyRepository);
    }

    // ========================================== DEACTIVATE ==========================================

    @Test
    void deactivate_userExists_savesUserAsInactive() {
        // Arrange
        UUID id = UUID.randomUUID();
        User existingUser = createSampleUser(id, "test@example.com", UserStatus.ACTIVO);
        Mockito.when(userRepository.findById(id)).thenReturn(Optional.of(existingUser));
        Mockito.when(userRepository.save(Mockito.any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        userService.deactivate(id);

        // Assert
        Mockito.verify(userRepository, Mockito.times(1)).save(Mockito.argThat(u -> u.getStatus() == UserStatus.INACTIVO));
    }

    @Test
    void deactivate_userDoesNotExist_throwsUserNotFoundException() {
        // Arrange
        UUID id = UUID.randomUUID();
        Mockito.when(userRepository.findById(id)).thenReturn(Optional.empty());

        // Act & Assert
        Assertions.assertThrows(UserNotFoundException.class, () -> userService.deactivate(id));
        Mockito.verify(userRepository, Mockito.never()).save(Mockito.any(User.class));
    }

    // ========================================== REACTIVATE ==========================================

    @Test
    void reactivate_userExists_savesUserAsActive() {
        // Arrange
        UUID id = UUID.randomUUID();
        User existingUser = createSampleUser(id, "test@example.com", UserStatus.INACTIVO);
        Mockito.when(userRepository.findById(id)).thenReturn(Optional.of(existingUser));
        Mockito.when(userRepository.save(Mockito.any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        userService.reactivate(id);

        // Assert
        Mockito.verify(userRepository, Mockito.times(1)).save(Mockito.argThat(u -> u.getStatus() == UserStatus.ACTIVO));
    }

    @Test
    void reactivate_userDoesNotExist_throwsUserNotFoundException() {
        // Arrange
        UUID id = UUID.randomUUID();
        Mockito.when(userRepository.findById(id)).thenReturn(Optional.empty());

        // Act & Assert
        Assertions.assertThrows(UserNotFoundException.class, () -> userService.reactivate(id));
        Mockito.verify(userRepository, Mockito.never()).save(Mockito.any(User.class));
    }

    // ========================================== FIND BY ID ==========================================

    @Test
    void findById_userExists_returnsUser() {
        // Arrange
        UUID id = UUID.randomUUID();
        User existingUser = createSampleUser(id, "test@example.com", UserStatus.ACTIVO);
        Mockito.when(userRepository.findById(id)).thenReturn(Optional.of(existingUser));

        // Act
        User result = userService.findById(id);

        // Assert
        Assertions.assertNotNull(result);
        Assertions.assertEquals(id, result.getId());
    }

    @Test
    void findById_userDoesNotExist_throwsUserNotFoundException() {
        // Arrange
        UUID id = UUID.randomUUID();
        Mockito.when(userRepository.findById(id)).thenReturn(Optional.empty());

        // Act & Assert
        Assertions.assertThrows(UserNotFoundException.class, () -> userService.findById(id));
    }

    // ========================================== LIST ==========================================

    @Test
    void list_delegatesToUserRepository() {
        // Arrange
        PageResult<User> expectedPage = new PageResult<>(Collections.emptyList(), 0, 0, 1);
        Mockito.when(userRepository.findAll("search", Role.ALUMNO, UserStatus.ACTIVO, 1, 10))
                .thenReturn(expectedPage);

        // Act
        PageResult<User> result = userService.list("search", Role.ALUMNO, UserStatus.ACTIVO, 1, 10);

        // Assert
        Assertions.assertSame(expectedPage, result);
        Mockito.verify(userRepository, Mockito.times(1)).findAll("search", Role.ALUMNO, UserStatus.ACTIVO, 1, 10);
    }
}
