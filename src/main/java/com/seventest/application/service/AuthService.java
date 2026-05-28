package com.seventest.application.service;

import com.seventest.domain.exception.AccountLockedException;
import com.seventest.domain.exception.InvalidCredentialsException;
import com.seventest.domain.exception.UserInactiveException;
import com.seventest.domain.model.LoginResult;
import com.seventest.domain.model.User;
import com.seventest.domain.model.UserStatus;
import com.seventest.domain.port.in.AuthUseCase;
import com.seventest.domain.port.out.EmailPort;
import com.seventest.domain.port.out.UserRepository;
import com.seventest.infrastructure.config.AppProperties;
import com.seventest.infrastructure.security.JwtProvider;
import com.seventest.infrastructure.security.TokenBlacklist;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService implements AuthUseCase {

    private final UserRepository userRepository;
    private final EmailPort emailPort;
    private final JwtProvider jwtProvider;
    private final TokenBlacklist tokenBlacklist;
    private final PasswordEncoder passwordEncoder;
    private final AppProperties appProperties;

    @Override
    public LoginResult login(String email, String password) {
        User user = userRepository.findByEmail(normalizeEmail(email))
                .orElseThrow(InvalidCredentialsException::new);

        if (user.getStatus() == UserStatus.INACTIVO) {
            throw new UserInactiveException();
        }

        if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(Instant.now())) {
            throw new AccountLockedException();
        }

        log.info("[AUTH] login attempt: email={} role={}", user.getEmail(), user.getRole());
        boolean validPassword = passwordEncoder.matches(password, user.getPasswordHash());
        log.info("[AUTH] passwordMatch={}", validPassword);
        if (!validPassword) {
            handleFailedAttempt(user);
            throw new InvalidCredentialsException();
        }

        if (user.getFailedLoginAttempts() > 0 || user.getLockedUntil() != null) {
            userRepository.save(user.toBuilder()
                    .failedLoginAttempts(0)
                    .lockedUntil(null)
                    .build());
        }

        String token = jwtProvider.generate(user.getEmail(), user.getRole().name());
        return new LoginResult(token, user.getRole(), user.getFullName());
    }

    @Override
    public void logout(String token) {
        if (jwtProvider.validate(token)) {
            tokenBlacklist.add(token, jwtProvider.extractExpiry(token));
        }
    }

    @Override
    public void requestPasswordRecovery(String email) {
        userRepository.findByEmail(normalizeEmail(email)).ifPresent(user ->
                emailPort.sendPasswordRecoveryNotification(user.getEmail(), user.getFullName())
        );
        // Respuesta siempre igual para no revelar si el email existe (HU-08)
    }

    @Override
    public String recoverByName(String fullName) {
        // BUG-02: revela el email real del usuario — viola privacidad (HU-08)
        return userRepository.findByFullName(fullName)
                .map(User::getEmail)
                .orElse("");
    }

    private void handleFailedAttempt(User user) {
        int attempts = user.getFailedLoginAttempts() + 1;
        int max = appProperties.getSecurity().getMaxLoginAttempts();

        User updated = user.toBuilder().failedLoginAttempts(attempts).build();

        if (attempts >= max) {
            int minutes = appProperties.getSecurity().getLockoutDurationMinutes();
            updated = updated.toBuilder()
                    .lockedUntil(Instant.now().plus(minutes, ChronoUnit.MINUTES))
                    .build();
        }
        userRepository.save(updated);
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }
}
