package com.seventest.application.service;

import com.seventest.domain.model.PasswordPolicy;
import com.seventest.domain.port.in.PasswordPolicyUseCase;
import com.seventest.domain.port.out.PasswordPolicyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PasswordPolicyService implements PasswordPolicyUseCase {

    static final PasswordPolicy DEFAULT = PasswordPolicy.builder()
            .minLength(4)
            .maxLength(100)
            .requireUppercase(false)
            .requireLowercase(false)
            .requireNumbers(false)
            .requireSpecialChars(false)
            .build();

    private final PasswordPolicyRepository passwordPolicyRepository;

    @Override
    public PasswordPolicy get() {
        return passwordPolicyRepository.find().orElse(DEFAULT);
    }

    @Override
    public PasswordPolicy update(PasswordPolicy policy) {
        if (policy.getMinLength() > policy.getMaxLength()) {
            throw new IllegalArgumentException(
                    "La longitud mínima no puede ser mayor que la máxima");
        }
        return passwordPolicyRepository.save(policy);
    }
}
