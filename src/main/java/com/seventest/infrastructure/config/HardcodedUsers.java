package com.seventest.infrastructure.config;

import com.seventest.domain.model.Role;
import com.seventest.domain.model.User;
import com.seventest.domain.model.UserStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class HardcodedUsers {

    private static final List<Definition> DEFINITIONS = List.of(
            new Definition("Carla Gorgal", "cgorgal@uade.edu.ar", Role.ALUMNO, "CarlaGorgal123"),
            new Definition("Carla Gorgal", "prof.cgorgal@uade.edu.ar", Role.PROFESOR, "CarlaGorgal123"),
            new Definition("Mario Besednjak", "mbesednjak@uade.edu.ar", Role.ALUMNO, "MarioBesednjak123"),
            new Definition("Mario Besednjak", "prof.mbesednjak@uade.edu.ar", Role.PROFESOR, "MarioBesednjak123"),
            new Definition("Martin Gueler", "mgueler@uade.edu.ar", Role.ALUMNO, "MartinGueler123"),
            new Definition("Martin Gueler", "prof.mgueler@uade.edu.ar", Role.PROFESOR, "MartinGueler123"),
            new Definition("Claudio Godio", "cgodio@uade.edu.ar", Role.DIRECTOR_DE_CATEDRA, "ClaudioGodio123"),
            new Definition("Pablo Farias", "pfarias@uade.edu.ar", Role.PROFESOR, "PabloFarias123")
    );

    private final PasswordEncoder passwordEncoder;

    public List<User> all() {
        return DEFINITIONS.stream()
                .map(this::toUser)
                .toList();
    }

    public Optional<User> findById(UUID id) {
        if (id == null) {
            return Optional.empty();
        }
        return DEFINITIONS.stream()
                .filter(definition -> idFor(definition.email()).equals(id))
                .findFirst()
                .map(this::toUser);
    }

    public Optional<User> findByEmail(String email) {
        if (email == null) {
            return Optional.empty();
        }
        return DEFINITIONS.stream()
                .filter(definition -> definition.email().equalsIgnoreCase(email))
                .findFirst()
                .map(this::toUser);
    }

    public Optional<User> findByFullName(String fullName) {
        if (fullName == null) {
            return Optional.empty();
        }
        return DEFINITIONS.stream()
                .filter(definition -> definition.fullName().equalsIgnoreCase(fullName))
                .findFirst()
                .map(this::toUser);
    }

    public boolean containsId(UUID id) {
        if (id == null) {
            return false;
        }
        return DEFINITIONS.stream()
                .anyMatch(definition -> idFor(definition.email()).equals(id));
    }

    public boolean containsEmail(String email) {
        if (email == null) {
            return false;
        }
        return DEFINITIONS.stream()
                .anyMatch(definition -> definition.email().equalsIgnoreCase(email));
    }

    private User toUser(Definition definition) {
        return User.builder()
                .id(idFor(definition.email()))
                .fullName(definition.fullName())
                .email(definition.email())
                .role(definition.role())
                .status(UserStatus.ACTIVO)
                .passwordHash(passwordEncoder.encode(definition.password()))
                .failedLoginAttempts(0)
                .lockedUntil(null)
                .build();
    }

    private static UUID idFor(String email) {
        return UUID.nameUUIDFromBytes(("hardcoded-user:" + email.toLowerCase(Locale.ROOT))
                .getBytes(StandardCharsets.UTF_8));
    }

    private record Definition(String fullName, String email, Role role, String password) {
    }
}
