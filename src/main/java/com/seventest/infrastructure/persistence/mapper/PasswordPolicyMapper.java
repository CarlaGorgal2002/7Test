package com.seventest.infrastructure.persistence.mapper;

import com.seventest.domain.model.PasswordPolicy;
import com.seventest.infrastructure.persistence.entity.PasswordPolicyEntity;

public class PasswordPolicyMapper {

    private PasswordPolicyMapper() {}

    public static PasswordPolicyEntity toEntity(PasswordPolicy policy) {
        return PasswordPolicyEntity.builder()
                .id(1L)
                .minLength(policy.getMinLength())
                .maxLength(policy.getMaxLength())
                .requireUppercase(policy.isRequireUppercase())
                .requireLowercase(policy.isRequireLowercase())
                .requireNumbers(policy.isRequireNumbers())
                .requireSpecialChars(policy.isRequireSpecialChars())
                .build();
    }

    public static PasswordPolicy toDomain(PasswordPolicyEntity entity) {
        return PasswordPolicy.builder()
                .minLength(entity.getMinLength())
                .maxLength(entity.getMaxLength())
                .requireUppercase(entity.isRequireUppercase())
                .requireLowercase(entity.isRequireLowercase())
                .requireNumbers(entity.isRequireNumbers())
                .requireSpecialChars(entity.isRequireSpecialChars())
                .build();
    }
}
