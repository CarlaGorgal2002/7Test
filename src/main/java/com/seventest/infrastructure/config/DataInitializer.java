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
        ensureAdmin();
    }

    private void ensureAdmin() {
        String email = "admin@seventest.local";
        String passwordHash = passwordEncoder.encode("Admin#7T$2026");
        userRepository.findByEmail(email).ifPresentOrElse(
                existing -> userRepository.save(existing.toBuilder()
                        .fullName("Administrador")
                        .role(Role.ADMINISTRADOR)
                        .status(UserStatus.ACTIVO)
                        .passwordHash(passwordHash)
                        .failedLoginAttempts(0)
                        .lockedUntil(null)
                        .build()),
                () -> userRepository.save(User.builder()
                        .id(UUID.randomUUID())
                        .fullName("Administrador")
                        .email(email)
                        .role(Role.ADMINISTRADOR)
                        .status(UserStatus.ACTIVO)
                        .passwordHash(passwordHash)
                        .failedLoginAttempts(0)
                        .lockedUntil(null)
                        .build())
        );
        log.info("Cuenta administradora preparada: {}", email);
    }
}
