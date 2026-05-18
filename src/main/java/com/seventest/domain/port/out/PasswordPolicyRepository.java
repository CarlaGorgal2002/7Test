package com.seventest.domain.port.out;

import com.seventest.domain.model.PasswordPolicy;

import java.util.Optional;

public interface PasswordPolicyRepository {
    Optional<PasswordPolicy> find();
    PasswordPolicy save(PasswordPolicy policy);
}
