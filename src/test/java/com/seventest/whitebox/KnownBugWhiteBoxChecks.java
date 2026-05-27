package com.seventest.whitebox;

import com.seventest.application.service.AuthService;
import com.seventest.domain.exception.InvalidCredentialsException;
import com.seventest.domain.model.Role;
import com.seventest.domain.model.User;
import com.seventest.domain.model.UserStatus;
import com.seventest.domain.port.out.EmailPort;
import com.seventest.domain.port.out.UserRepository;
import com.seventest.infrastructure.config.AppProperties;
import com.seventest.infrastructure.security.JwtProvider;
import com.seventest.infrastructure.security.TokenBlacklist;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.Mockito.when;

/**
 * These checks intentionally describe the correct expected behavior.
 *
 * The class name does not end with Test, so Maven does not run it in the
 * normal green suite. Execute it explicitly when you need evidence of the
 * seeded defects:
 *
 *   mvnw.cmd -Dtest=KnownBugWhiteBoxChecks test
 */
@ExtendWith(MockitoExtension.class)
class KnownBugWhiteBoxChecks {

    @Mock UserRepository userRepository;
    @Mock EmailPort emailPort;
    @Mock JwtProvider jwtProvider;
    @Mock TokenBlacklist tokenBlacklist;
    @Mock PasswordEncoder passwordEncoder;
    @Mock AppProperties appProperties;

    @InjectMocks AuthService authService;

    private User activeUser(String fullName, String email, String hash) {
        return User.builder()
                .id(UUID.randomUUID())
                .fullName(fullName)
                .email(email)
                .role(Role.ALUMNO)
                .status(UserStatus.ACTIVO)
                .passwordHash(hash)
                .failedLoginAttempts(0)
                .lockedUntil(null)
                .build();
    }

    private String loginPageSource() throws IOException {
        return Files.readString(Path.of("frontend", "src", "pages", "LoginPage.jsx"), StandardCharsets.UTF_8);
    }

    @Test
    void loginNoAdmin_conPasswordDeOtroUsuario_deberiaRechazarCredenciales() {
        User userA = activeUser("Usuario A", "a@test.com", "hashA");
        when(userRepository.findByEmail("a@test.com")).thenReturn(Optional.of(userA));
        when(passwordEncoder.matches("passwordB", "hashA")).thenReturn(false);
        AppProperties.Security security = new AppProperties.Security();
        security.setMaxLoginAttempts(5);
        security.setLockoutDurationMinutes(15);
        when(appProperties.getSecurity()).thenReturn(security);

        assertThatThrownBy(() -> authService.login("a@test.com", "passwordB"))
                .as("HU-05: el email de A con la password de B no deberia autenticar a A")
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void loginPage_noDeberiaContenerErrorOrtograficoEnPassword() throws IOException {
        boolean hasTypo = loginPageSource().contains("Contrace");

        assertThat(hasTypo)
                .as("Bug cosmetico solicitado: aparece 'Contrace...' en el label")
                .isFalse();
    }

    @Test
    void loginPage_deberiaMantenerTextosVisiblesEnEspanol() throws IOException {
        String source = loginPageSource();

        assertSoftly(softly -> {
            softly.assertThat(source.contains("'Login'"))
                    .as("Boton principal")
                    .isFalse();
            softly.assertThat(source.contains("'Loading...'"))
                    .as("Estado de carga")
                    .isFalse();
            softly.assertThat(source.contains("Cerrar session") || source.contains("Cerrar seccion"))
                    .as("Texto visible de cierre de sesion")
                    .isFalse();
        });
    }

    @Test
    void loginPage_modoOscuro_deberiaUsarColorDeTextoContrastante() throws IOException {
        String source = loginPageSource();

        assertSoftly(softly -> {
            softly.assertThat(source.contains("          color: #122430 !important;"))
                    .as("El color del texto no debe ser igual al fondo oscuro")
                    .isFalse();
            softly.assertThat(source.contains("-webkit-text-fill-color: #122430 !important"))
                    .as("El color de autofill tampoco debe mimetizarse con el fondo")
                    .isFalse();
        });
    }
}
