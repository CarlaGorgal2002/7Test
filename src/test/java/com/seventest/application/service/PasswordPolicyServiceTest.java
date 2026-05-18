package com.seventest.application.service;

import com.seventest.domain.model.PasswordPolicy;
import com.seventest.domain.port.out.PasswordPolicyRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PasswordPolicyServiceTest {

    @Mock PasswordPolicyRepository passwordPolicyRepository;

    @InjectMocks PasswordPolicyService passwordPolicyService;

    private PasswordPolicy validPolicy(int min, int max) {
        return PasswordPolicy.builder()
                .minLength(min).maxLength(max)
                .requireUppercase(false).requireLowercase(false)
                .requireNumbers(false).requireSpecialChars(false)
                .build();
    }

    // ----------------------------------------------------------------- get
    @Test
    void get_cuandoExistePolitica_laRetorna() {
        PasswordPolicy stored = validPolicy(8, 50);
        when(passwordPolicyRepository.find()).thenReturn(Optional.of(stored));

        PasswordPolicy result = passwordPolicyService.get();

        assertThat(result.getMinLength()).isEqualTo(8);
    }

    @Test
    void get_cuandoNohayPolitica_retornaDefault() {
        when(passwordPolicyRepository.find()).thenReturn(Optional.empty());

        PasswordPolicy result = passwordPolicyService.get();

        assertThat(result.getMinLength()).isEqualTo(PasswordPolicyService.DEFAULT.getMinLength());
        assertThat(result.getMaxLength()).isEqualTo(PasswordPolicyService.DEFAULT.getMaxLength());
    }

    // --------------------------------------------------------------- update
    @Test
    void update_conPoliticaValida_guardaYRetorna() {
        PasswordPolicy policy = validPolicy(8, 64);
        when(passwordPolicyRepository.save(policy)).thenReturn(policy);

        PasswordPolicy result = passwordPolicyService.update(policy);

        assertThat(result.getMinLength()).isEqualTo(8);
        verify(passwordPolicyRepository).save(policy);
    }

    @Test
    void update_conMinMayorQueMax_lanzaIllegalArgument() {
        PasswordPolicy invalid = validPolicy(20, 10);

        assertThatThrownBy(() -> passwordPolicyService.update(invalid))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("mínima");

        verifyNoInteractions(passwordPolicyRepository);
    }

    @Test
    void update_conMinIgualAMax_esValido() {
        PasswordPolicy policy = validPolicy(8, 8);
        when(passwordPolicyRepository.save(policy)).thenReturn(policy);

        PasswordPolicy result = passwordPolicyService.update(policy);

        assertThat(result.getMinLength()).isEqualTo(result.getMaxLength());
    }
}
