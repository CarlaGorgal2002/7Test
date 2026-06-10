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

import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    static final List<AccountPair> ACCOUNT_PAIRS = List.of(
            pair("Tomas Agostinelli", "tagostinelli", "TomasAgostinelli123"),
            pair("Angel Aldana", "aaldana", "AngelAldana123"),
            pair("Mauricio Antolin", "mantolin", "MauricioAntolin123"),
            pair("Ricardo Areiza", "rareiza", "RicardoAreiza123"),
            pair("Shimon Bacari", "sbacari", "ShimonBacari123"),
            pair("Mario Besednjak", "mbesednjak", "MarioBesednjak123"),
            pair("Matias Bonura", "mbonura", "MatiasBonura123"),
            pair("Nelson Carreno", "ncarreno", "NelsonCarreno123"),
            pair("Nicolas Castro", "ncastro", "NicolasCastro123"),
            pair("Vicente Cesareo", "vcesareo", "VicenteCesareo123"),
            pair("Maria Constan", "mconstan", "MariaConstan123"),
            pair("Esthefany Contreras", "econtreras", "EsthefanyContreras123"),
            pair("Francisco Cravello", "fcravello", "FranciscoCravello123"),
            pair("Bautista Cremona", "bcremona", "BautistaCremona123"),
            pair("Lautaro Diaz", "ldiaz", "LautaroDiaz123"),
            pair("Brian Duran", "bduran", "BrianDuran123"),
            pair("Eric Epstein", "eepstein", "EricEpstein123"),
            pair("Mariano Fina", "mfina", "MarianoFina123"),
            pair("Gianluca Francisco", "gfrancisco", "GianlucaFrancisco123"),
            pair("Valentino Gasipi", "vgasipi", "ValentinoGasipi123"),
            pair("Lorenzo Giussani", "lgiussani", "LorenzoGiussani123"),
            pair("Carla Gorgal", "cgorgal", "VayanseTodosALaMierda20021995"),
            pair("Lucia Goyer", "lgoyer", "LuciaGoyer123"),
            pair("Federico Grasso", "fgrasso", "FedericoGrasso123"),
            pair("Martin Gueler", "mgueler", "MartinGueler123"),
            pair("Tobias Hernandez", "thernandez", "TobiasHernandez123"),
            pair("Alejo Iparraguirre", "aiparraguirre", "AlejoIparraguirre123"),
            pair("Pedro Larranaga", "plarranaga", "PedroLarranaga123"),
            pair("Carlos Lombardo", "clombardo", "CarlosLombardo123"),
            pair("Nicolas Martinez", "nmartinez", "NicolasMartinez123"),
            pair("Facundo Mello", "fmello", "FacundoMello123"),
            pair("Jean Paul Peralta", "jperalta", "JeanPaulPeralta123"),
            pair("Gonzalo Perez", "gperez", "GonzaloPerez123"),
            pair("Lautaro Perilli", "lperilli", "LautaroPerilli123"),
            pair("Santino Petrone", "spetrone", "SantinoPetrone123"),
            pair("Carlos Ramos", "cramos", "CarlosRamos123"),
            pair("Gregorio Reartes", "greartes", "GregorioReartes123"),
            pair("Stephanie Reynolds", "sreynolds", "StephanieReynolds123"),
            pair("Victoria Rodriguez", "vrodriguez", "VictoriaRodriguez123"),
            new AccountPair("Matias Romano", "mromano@uade.edu.ar", "prof.mromanosc@uade.edu.ar", "MatiasRomano123"),
            pair("Martina Romero", "mromero", "MartinaRomero123"),
            pair("Ezra Safadie", "esafadie", "EzraSafadie123"),
            pair("Valentina Servidio", "vservidio", "ValentinaServidio123"),
            pair("Emiliano Taborda", "etaborda", "EmilianoTaborda123"),
            pair("Franco Vechio", "fvechio", "FrancoVechio123")
    );

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        ensureAdmin();
        ACCOUNT_PAIRS.forEach(pair -> {
            ensureUser(pair.fullName(), pair.studentEmail(), Role.ALUMNO, pair.password());
            ensureUser(pair.fullName(), pair.teacherEmail(), Role.PROFESOR, pair.password());
        });
    }

    private void ensureAdmin() {
        ensureUser("Administrador", "admin@seventest.local", Role.ADMINISTRADOR, "Admin#7T$2026");
    }

    private void ensureUser(String fullName, String email, Role role, String password) {
        String passwordHash = passwordEncoder.encode(password);
        userRepository.findByEmail(email).ifPresentOrElse(
                existing -> userRepository.save(existing.toBuilder()
                        .fullName(fullName)
                        .role(role)
                        .status(UserStatus.ACTIVO)
                        .passwordHash(passwordHash)
                        .failedLoginAttempts(0)
                        .lockedUntil(null)
                        .build()),
                () -> userRepository.save(User.builder()
                        .id(UUID.randomUUID())
                        .fullName(fullName)
                        .email(email)
                        .role(role)
                        .status(UserStatus.ACTIVO)
                        .passwordHash(passwordHash)
                        .failedLoginAttempts(0)
                        .lockedUntil(null)
                        .build())
        );
        log.info("Cuenta semilla preparada: {}", email);
    }

    private static AccountPair pair(String fullName, String localPart, String password) {
        return new AccountPair(fullName, localPart + "@uade.edu.ar", "prof." + localPart + "@uade.edu.ar", password);
    }

    record AccountPair(String fullName, String studentEmail, String teacherEmail, String password) {}
}
