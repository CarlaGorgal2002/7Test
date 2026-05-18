package com.seventest.domain.model;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PasswordPolicy {
    private final int minLength;
    private final int maxLength;
    private final boolean requireUppercase;
    private final boolean requireLowercase;
    private final boolean requireNumbers;
    private final boolean requireSpecialChars;
}
