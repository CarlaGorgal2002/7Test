package com.seventest.application.service;

import com.seventest.domain.model.PasswordPolicy;
import com.seventest.domain.port.out.PasswordPolicyRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class PasswordPolicyServiceTest {

    @Mock
    private PasswordPolicyRepository passwordPolicyRepository;

    @InjectMocks
    private PasswordPolicyService passwordPolicyService;

    @Test
    void get_whenPolicyExists_returnsStoredPolicy() {
        // Arrange
        PasswordPolicy storedPolicy = PasswordPolicy.builder()
                .minLength(8)
                .maxLength(30)
                .build();
        Mockito.when(passwordPolicyRepository.find()).thenReturn(Optional.of(storedPolicy));

        // Act
        PasswordPolicy result = passwordPolicyService.get();

        // Assert
        Assertions.assertNotNull(result);
        Assertions.assertEquals(8, result.getMinLength());
        Assertions.assertEquals(30, result.getMaxLength());
        Mockito.verify(passwordPolicyRepository, Mockito.times(1)).find();
    }

    @Test
    void get_whenPolicyDoesNotExist_returnsDefaultPolicy() {
        // Arrange
        Mockito.when(passwordPolicyRepository.find()).thenReturn(Optional.empty());

        // Act
        PasswordPolicy result = passwordPolicyService.get();

        // Assert
        Assertions.assertNotNull(result);
        Assertions.assertEquals(PasswordPolicyService.DEFAULT.getMinLength(), result.getMinLength());
        Assertions.assertEquals(PasswordPolicyService.DEFAULT.getMaxLength(), result.getMaxLength());
        Mockito.verify(passwordPolicyRepository, Mockito.times(1)).find();
    }

    @Test
    void update_withValidPolicy_savesAndReturnsPolicy() {
        // Arrange
        PasswordPolicy policy = PasswordPolicy.builder()
                .minLength(6)
                .maxLength(20)
                .build();
        Mockito.when(passwordPolicyRepository.save(policy)).thenReturn(policy);

        // Act
        PasswordPolicy result = passwordPolicyService.update(policy);

        // Assert
        Assertions.assertNotNull(result);
        Assertions.assertEquals(6, result.getMinLength());
        Assertions.assertEquals(20, result.getMaxLength());
        Mockito.verify(passwordPolicyRepository, Mockito.times(1)).save(policy);
    }

    @Test
    void update_withMinLengthGreaterThanMaxLength_throwsIllegalArgumentException() {
        // Arrange
        PasswordPolicy invalidPolicy = PasswordPolicy.builder()
                .minLength(20)
                .maxLength(10)
                .build();

        // Act & Assert
        IllegalArgumentException exception = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> passwordPolicyService.update(invalidPolicy)
        );
        Assertions.assertEquals("La longitud mínima no puede ser mayor que la máxima", exception.getMessage());
        Mockito.verifyNoInteractions(passwordPolicyRepository);
    }

    @Test
    void update_withMinLengthEqualMaxLength_savesAndReturnsPolicy() {
        // Arrange
        PasswordPolicy policy = PasswordPolicy.builder()
                .minLength(10)
                .maxLength(10)
                .build();
        Mockito.when(passwordPolicyRepository.save(policy)).thenReturn(policy);

        // Act
        PasswordPolicy result = passwordPolicyService.update(policy);

        // Assert
        Assertions.assertNotNull(result);
        Assertions.assertEquals(10, result.getMinLength());
        Assertions.assertEquals(10, result.getMaxLength());
        Mockito.verify(passwordPolicyRepository, Mockito.times(1)).save(policy);
    }
}
