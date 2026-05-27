package com.seventest.application.service;

import com.seventest.domain.model.Exam;
import com.seventest.domain.model.ExamQuestion;
import com.seventest.domain.model.ExamStatus;
import com.seventest.domain.model.ExamTopic;
import com.seventest.domain.model.Role;
import com.seventest.domain.model.User;
import com.seventest.domain.model.UserStatus;
import com.seventest.domain.port.out.ExamRepository;
import com.seventest.domain.port.out.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExamServiceTest {

    @Mock ExamRepository examRepository;
    @Mock UserRepository userRepository;

    @InjectMocks ExamService examService;

    @Test
    void crearExamen_profesorValido_guardaBorrador() {
        User teacher = teacher();
        when(userRepository.findByEmail(teacher.getEmail())).thenReturn(Optional.of(teacher));
        when(examRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Exam result = examService.create(teacher.getEmail(), "Primer parcial", "Texto libre", null, null, 120);

        assertThat(result.getStatus()).isEqualTo(ExamStatus.BORRADOR);
        assertThat(result.getTeacherId()).isEqualTo(teacher.getId());
        assertThat(result.getTitle()).isEqualTo("Primer parcial");
        assertThat(result.getCourseName()).isEqualTo("Testing de Aplicaciones");
        assertThat(result.getTopics()).isEmpty();
    }

    @Test
    void agregarTemas_asignaColoresDelSprintDos() {
        User teacher = teacher();
        Exam exam = exam(teacher, ExamStatus.BORRADOR, List.of());
        when(userRepository.findByEmail(teacher.getEmail())).thenReturn(Optional.of(teacher));
        when(examRepository.findById(exam.getId())).thenReturn(Optional.of(exam));
        when(examRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Exam result = examService.addTopic(teacher.getEmail(), exam.getId(), "Tema A");

        assertThat(result.getTopics()).hasSize(1);
        assertThat(result.getTopics().get(0).getColorHex()).isEqualTo("#1956D8");
    }

    @Test
    void publicarExamen_conTemaQueNoSumaDiez_rechazaPublicacion() {
        User teacher = teacher();
        Exam exam = exam(teacher, ExamStatus.BORRADOR, List.of(topic("Tema A", question("P1", "R1", "4"))));
        when(userRepository.findByEmail(teacher.getEmail())).thenReturn(Optional.of(teacher));
        when(examRepository.findById(exam.getId())).thenReturn(Optional.of(exam));

        assertThatThrownBy(() -> examService.publish(teacher.getEmail(), exam.getId()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("10 puntos");
    }

    @Test
    void publicarExamen_conCadaTemaEnDiez_cambiaEstadoAPublicado() {
        User teacher = teacher();
        Exam exam = exam(teacher, ExamStatus.BORRADOR, List.of(
                topic("Tema A", question("P1", "R1", "6"), question("P2", "R2", "4")),
                topic("Tema B", question("P1", "R1", "10"))
        ));
        when(userRepository.findByEmail(teacher.getEmail())).thenReturn(Optional.of(teacher));
        when(examRepository.findById(exam.getId())).thenReturn(Optional.of(exam));
        when(examRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Exam result = examService.publish(teacher.getEmail(), exam.getId());

        assertThat(result.getStatus()).isEqualTo(ExamStatus.PUBLICADO);
        assertThat(result.getPublishedAt()).isNotNull();
    }

    @Test
    void agregarPregunta_aExamenPublicado_rechazaEdicion() {
        User teacher = teacher();
        ExamTopic topic = topic("Tema A", question("P1", "R1", "10"));
        Exam exam = exam(teacher, ExamStatus.PUBLICADO, List.of(topic));
        when(userRepository.findByEmail(teacher.getEmail())).thenReturn(Optional.of(teacher));
        when(examRepository.findById(exam.getId())).thenReturn(Optional.of(exam));

        assertThatThrownBy(() -> examService.addQuestion(
                teacher.getEmail(), exam.getId(), topic.getId(), "Nueva", "Modelo", BigDecimal.ONE))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("borrador");
    }

    private User teacher() {
        return User.builder()
                .id(UUID.randomUUID())
                .fullName("Profesora Test")
                .email("profe@test.com")
                .role(Role.PROFESOR)
                .status(UserStatus.ACTIVO)
                .passwordHash("hash")
                .failedLoginAttempts(0)
                .lockedUntil(null)
                .build();
    }

    private Exam exam(User teacher, ExamStatus status, List<ExamTopic> topics) {
        Instant now = Instant.now();
        return Exam.builder()
                .id(UUID.randomUUID())
                .title("Parcial")
                .description("")
                .courseName("Testing de Aplicaciones")
                .teacherId(teacher.getId())
                .teacherName(teacher.getFullName())
                .status(status)
                .durationMinutes(120)
                .topics(topics)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    private ExamTopic topic(String name, ExamQuestion... questions) {
        return ExamTopic.builder()
                .id(UUID.randomUUID())
                .name(name)
                .colorHex("#1956D8")
                .questions(List.of(questions))
                .build();
    }

    private ExamQuestion question(String prompt, String modelAnswer, String points) {
        return ExamQuestion.builder()
                .id(UUID.randomUUID())
                .prompt(prompt)
                .modelAnswer(modelAnswer)
                .points(new BigDecimal(points))
                .displayOrder(1)
                .build();
    }
}
