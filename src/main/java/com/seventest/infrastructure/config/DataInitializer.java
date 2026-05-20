package com.seventest.infrastructure.config;

import com.seventest.domain.model.Role;
import com.seventest.domain.model.User;
import com.seventest.domain.model.UserStatus;
import com.seventest.domain.port.out.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        ensureUser("Administrador", "admin@seventest.local", Role.ADMINISTRADOR, "Admin#7T$2026");
    }

    private void ensureUser(String fullName, String email, Role role, String password) {
        userRepository.findByEmail(email).ifPresentOrElse(
                existing -> {
                    userRepository.save(existing.toBuilder()
                            .fullName(fullName)
                            .role(role)
                            .status(UserStatus.ACTIVO)
                            .passwordHash(passwordEncoder.encode(password))
                            .failedLoginAttempts(0)
                            .lockedUntil(null)
                            .build());
                    log.info("Usuario semilla actualizado y activo: {}", email);
                },
                () -> {
                    userRepository.save(User.builder()
                            .id(UUID.randomUUID())
                            .fullName(fullName)
                            .email(email)
                            .role(role)
                            .status(UserStatus.ACTIVO)
                            .passwordHash(passwordEncoder.encode(password))
                            .failedLoginAttempts(0)
                            .lockedUntil(null)
                            .build());
                    log.info("Usuario semilla creado: {}", email);
                }
        );
    }
}
