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

        ensureStudentOnce("Nelson Carreño",            "NelsonCarreno@uade.edu.ar",       "NelsonCarreno123");
        ensureStudentOnce("Brian Durán Vargas",        "BrianDuran@uade.edu.ar",          "BrianDuran123");
        ensureStudentOnce("Carlos Ramos Verón",        "CarlosRamos@uade.edu.ar",         "CarlosRamos123");
        ensureStudentOnce("Tomas Agostinelli",         "TomasAgostinelli@uade.edu.ar",    "TomasAgostinelli123");
        ensureStudentOnce("Federico Grasso",           "FedericoGrasso@uade.edu.ar",      "FedericoGrasso123");
        ensureStudentOnce("Jean Paul Peralta Prada",   "JeanPaulPeralta@uade.edu.ar",     "JeanPaulPeralta123");
        ensureStudentOnce("Shimon Bacari",             "ShimonBacari@uade.edu.ar",        "ShimonBacari123");
        ensureStudentOnce("Lorenzo Giussani",          "LorenzoGiussani@uade.edu.ar",     "LorenzoGiussani123");
        ensureStudentOnce("Gianluca Francisco",        "GianlucaFrancisco@uade.edu.ar",   "GianlucaFrancisco123");
        ensureStudentOnce("Angel Aldana Pazos",        "AngelAldana@uade.edu.ar",         "AngelAldana123");
        ensureStudentOnce("Gonzalo Pérez Grunau",      "GonzaloPerez@uade.edu.ar",        "GonzaloPerez123");
        ensureStudentOnce("Victoria Rodríguez",        "VictoriaRodriguez@uade.edu.ar",   "VictoriaRodriguez123");
        ensureStudentOnce("Alejo Iparraguirre",        "AlejoIparraguirre@uade.edu.ar",   "AlejoIparraguirre123");
        ensureStudentOnce("Francisco Cravello",        "FranciscoCravello@uade.edu.ar",   "FranciscoCravello123");
        ensureStudentOnce("Santino Petrone",           "SantinoPetrone@uade.edu.ar",      "SantinoPetrone123");
        ensureStudentOnce("Valentina Servidio",        "ValentinaServidio@uade.edu.ar",   "ValentinaServidio123");
        ensureStudentOnce("Eric Epstein",              "EricEpstein@uade.edu.ar",         "EricEpstein123");
        ensureStudentOnce("Tobias Hernández",          "TobiasHernandez@uade.edu.ar",     "TobiasHernandez123");
        ensureStudentOnce("Ricardo Areiza",            "RicardoAreiza@uade.edu.ar",       "RicardoAreiza123");
        ensureStudentOnce("Nicolas Castro",            "NicolasCastro@uade.edu.ar",       "NicolasCastro123");
        ensureStudentOnce("Mauricio Antolin",          "MauricioAntolin@uade.edu.ar",     "MauricioAntolin123");
        ensureStudentOnce("Bautista Cremona",          "BautistaCremona@uade.edu.ar",     "BautistaCremona123");
        ensureStudentOnce("Mariano Fina",              "MarianoFina@uade.edu.ar",         "MarianoFina123");
        ensureStudentOnce("Valentino Gasipi",          "ValentinoGasipi@uade.edu.ar",     "ValentinoGasipi123");
        ensureStudentOnce("Stephanie Reynolds Bach",   "StephanieReynolds@uade.edu.ar",   "StephanieReynolds123");
        ensureStudentOnce("Emiliano Taborda",          "EmilianoTaborda@uade.edu.ar",     "EmilianoTaborda123");
        ensureStudentOnce("Esthefany Contreras",       "EsthefanyContreras@uade.edu.ar",  "EsthefanyContreras123");
        ensureStudentOnce("Vicente Cesareo",           "VicenteCesareo@uade.edu.ar",      "VicenteCesareo123");
        ensureStudentOnce("Lautaro Diaz",              "LautaroDiaz@uade.edu.ar",         "LautaroDiaz123");
        ensureStudentOnce("Lautaro Perilli",           "LautaroPerilli@uade.edu.ar",      "LautaroPerilli123");
        ensureStudentOnce("Gregorio Reartes",          "GregorioReartes@uade.edu.ar",     "GregorioReartes123");
        ensureStudentOnce("Matias Romano",             "MatiasRomano@uade.edu.ar",        "MatiasRomano123");
        ensureStudentOnce("Martina Romero",            "MartinaRomero@uade.edu.ar",       "MartinaRomero123");
        ensureStudentOnce("Nicolas Martinez",          "NicolasMartinez@uade.edu.ar",     "NicolasMartinez123");
        ensureStudentOnce("Pedro Larrañaga",           "PedroLarranaga@uade.edu.ar",      "PedroLarranaga123");
        ensureStudentOnce("Maria Constan Langer",      "MariaConstan@uade.edu.ar",        "MariaConstan123");
        ensureStudentOnce("Carlos Lombardo",           "CarlosLombardo@uade.edu.ar",      "CarlosLombardo123");
        ensureStudentOnce("Franco Vechio",             "FrancoVechio@uade.edu.ar",        "FrancoVechio123");
        ensureStudentOnce("Lucia Goyer",               "LuciaGoyer@uade.edu.ar",          "LuciaGoyer123");
        ensureStudentOnce("Matias Bonura Oyarse",      "MatiasBonura@uade.edu.ar",        "MatiasBonura123");
        ensureStudentOnce("Ezra Safadie",              "EzraSafadie@uade.edu.ar",         "EzraSafadie123");
        ensureStudentOnce("Facundo Mello Ferreira",    "FacundoMello@uade.edu.ar",        "FacundoMello123");
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
                    log.info("Usuario semilla actualizado: {}", email);
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

    private void ensureStudentOnce(String fullName, String email, String password) {
        if (userRepository.existsByEmail(email)) return;
        userRepository.save(User.builder()
                .id(UUID.randomUUID())
                .fullName(fullName)
                .email(email)
                .role(Role.ALUMNO)
                .status(UserStatus.ACTIVO)
                .passwordHash(passwordEncoder.encode(password))
                .failedLoginAttempts(0)
                .lockedUntil(null)
                .build());
        log.info("Alumno creado: {}", email);
    }
}
