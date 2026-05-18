package com.seventest.domain.model;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder(toBuilder = true)
public class User {
    private final UUID id;
    private final String fullName;
    private final String email;
    private final Role role;
    private final UserStatus status;
    private final String passwordHash;
    private final int failedLoginAttempts;
    private final Instant lockedUntil;
}
