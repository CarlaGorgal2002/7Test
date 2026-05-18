package com.seventest.domain.port.in;

import com.seventest.domain.model.PasswordPolicy;

public interface PasswordPolicyUseCase {
    PasswordPolicy get();
    PasswordPolicy update(PasswordPolicy policy);
}
