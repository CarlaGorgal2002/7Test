package com.seventest.infrastructure.persistence.mapper;

import com.seventest.domain.model.User;
import com.seventest.infrastructure.persistence.entity.UserEntity;

public class UserMapper {

    private UserMapper() {}

    public static UserEntity toEntity(User user) {
        return UserEntity.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .role(user.getRole())
                .status(user.getStatus())
                .passwordHash(user.getPasswordHash())
                .failedLoginAttempts(user.getFailedLoginAttempts())
                .lockedUntil(user.getLockedUntil())
                .build();
    }

    public static User toDomain(UserEntity entity) {
        return User.builder()
                .id(entity.getId())
                .fullName(entity.getFullName())
                .email(entity.getEmail())
                .role(entity.getRole())
                .status(entity.getStatus())
                .passwordHash(entity.getPasswordHash())
                .failedLoginAttempts(entity.getFailedLoginAttempts())
                .lockedUntil(entity.getLockedUntil())
                .build();
    }
}
