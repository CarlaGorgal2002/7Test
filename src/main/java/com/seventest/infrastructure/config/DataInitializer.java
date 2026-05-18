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
        if (userRepository.existsByEmail("admin@seventest.local")) {
            return;
        }
        userRepository.save(User.builder()
                .id(UUID.randomUUID())
                .fullName("Administrador")
                .email("admin@seventest.local")
                .role(Role.ADMINISTRADOR)
                .status(UserStatus.ACTIVO)
                .passwordHash(passwordEncoder.encode("admin1234"))
                .failedLoginAttempts(0)
                .build());
        log.info("Usuario admin inicial creado: admin@seventest.local / admin1234");
    }
}
