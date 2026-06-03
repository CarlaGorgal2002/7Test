package com.seventest.application.service;

import com.seventest.domain.model.Exam;
import com.seventest.domain.model.ExamQuestion;
import com.seventest.domain.model.ExamStatus;
import com.seventest.domain.model.ExamTopic;
import com.seventest.domain.model.Role;
import com.seventest.domain.model.User;
import com.seventest.domain.model.UserStatus;
import com.seventest.domain.port.out.ExamRepository;
import com.seventest.domain.port.out.ExamSubmissionRepository;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExamServiceTest {

    @Mock ExamRepository examRepository;
    @Mock UserRepository userRepository;
    @Mock ExamSubmissionRepository submissionRepository;

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
    void listarExamenesProfesor_traeSoloLosDelProfesorAutenticado() {
        User teacher = teacher("profesor-a@test.com");
        Exam ownDraft = exam(teacher, ExamStatus.BORRADOR, List.of());
        Exam ownPublished = exam(teacher, ExamStatus.PUBLICADO, List.of(topic("Tema A", question("P1", "R1", "10"))));
        when(userRepository.findByEmail(teacher.getEmail())).thenReturn(Optional.of(teacher));
        when(examRepository.findByTeacherId(teacher.getId())).thenReturn(List.of(ownDraft, ownPublished));

        List<Exam> result = examService.listForTeacher(teacher.getEmail());

        assertThat(result).containsExactly(ownDraft, ownPublished);
        assertThat(result).allMatch(exam -> exam.getTeacherId().equals(teacher.getId()));
        verify(examRepository).findByTeacherId(teacher.getId());
    }

    @Test
    void listarPublicadosParaAlumnos_traeExamenesPublicadosDeTodosLosProfesores() {
        User teacherA = teacher("profesor-a@test.com");
        User teacherB = teacher("profesor-b@test.com");
        Exam publishedByA = exam(teacherA, ExamStatus.PUBLICADO, List.of(topic("Tema A", question("P1", "R1", "10"))));
        Exam publishedByB = exam(teacherB, ExamStatus.PUBLICADO, List.of(topic("Tema A", question("P1", "R1", "10"))));
        Exam closedByB = exam(teacherB, ExamStatus.CERRADO, List.of(topic("Tema A", question("P1", "R1", "10"))));
        when(examRepository.findByStatus(ExamStatus.PUBLICADO)).thenReturn(List.of(publishedByA, publishedByB));
        when(examRepository.findByStatus(ExamStatus.CERRADO)).thenReturn(List.of(closedByB));

        List<Exam> result = examService.listPublishedForStudents();

        assertThat(result).containsExactly(publishedByA, publishedByB, closedByB);
    }

    @Test
    void cerrarExamen_deOtroProfesor_rechazaOperacion() {
        User owner = teacher("profesor-duenio@test.com");
        User otherTeacher = teacher("profesor-ajeno@test.com");
        Exam exam = exam(owner, ExamStatus.PUBLICADO, List.of(topic("Tema A", question("P1", "R1", "10"))));
        when(userRepository.findByEmail(otherTeacher.getEmail())).thenReturn(Optional.of(otherTeacher));
        when(examRepository.findById(exam.getId())).thenReturn(Optional.of(exam));

        assertThatThrownBy(() -> examService.close(otherTeacher.getEmail(), exam.getId()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("otro profesor");
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

    @Test
    void agregarPregunta_enBorradorPermiteEspaciosVacios() {
        User teacher = teacher();
        ExamTopic topic = topic("Tema A");
        Exam exam = exam(teacher, ExamStatus.BORRADOR, List.of(topic));
        when(userRepository.findByEmail(teacher.getEmail())).thenReturn(Optional.of(teacher));
        when(examRepository.findById(exam.getId())).thenReturn(Optional.of(exam));
        when(examRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Exam result = examService.addQuestion(teacher.getEmail(), exam.getId(), topic.getId(), "", "", BigDecimal.ONE);

        assertThat(result.getTopics().get(0).getQuestions()).hasSize(1);
        assertThat(result.getTopics().get(0).getQuestions().get(0).getPrompt()).isEmpty();
        assertThat(result.getTopics().get(0).getQuestions().get(0).getModelAnswer()).isEmpty();
    }

    @Test
    void agregarPregunta_siTemaSuperaDiez_permiteCambioParaRedistribucion() {
        User teacher = teacher();
        ExamTopic topic = topic("Tema A", question("P1", "R1", "9"));
        Exam exam = exam(teacher, ExamStatus.BORRADOR, List.of(topic));
        when(userRepository.findByEmail(teacher.getEmail())).thenReturn(Optional.of(teacher));
        when(examRepository.findById(exam.getId())).thenReturn(Optional.of(exam));
        when(examRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Exam result = examService.addQuestion(teacher.getEmail(), exam.getId(), topic.getId(), "P2", "R2", new BigDecimal("2"));

        assertThat(result.getTopics().get(0).getQuestions()).hasSize(2);
    }

    @Test
    void publicarExamen_conPreguntaVacia_rechazaPublicacion() {
        User teacher = teacher();
        Exam exam = exam(teacher, ExamStatus.BORRADOR, List.of(topic("Tema A", question("", "", "10"))));
        when(userRepository.findByEmail(teacher.getEmail())).thenReturn(Optional.of(teacher));
        when(examRepository.findById(exam.getId())).thenReturn(Optional.of(exam));

        assertThatThrownBy(() -> examService.publish(teacher.getEmail(), exam.getId()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("enunciado");
    }

    @Test
    void publicarExamen_conArbolVacio_rechazaPublicacion() {
        User teacher = teacher();
        Exam exam = exam(teacher, ExamStatus.BORRADOR, List.of(topic("Tema A",
                question("Arbol", "7TEST_DECISION_TREE:{\"nodes\":[],\"edges\":[]}", "10"))));
        when(userRepository.findByEmail(teacher.getEmail())).thenReturn(Optional.of(teacher));
        when(examRepository.findById(exam.getId())).thenReturn(Optional.of(exam));

        assertThatThrownBy(() -> examService.publish(teacher.getEmail(), exam.getId()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("respuesta modelo");
    }

    @Test
    void publicarExamen_conTablaVacia_rechazaPublicacion() {
        User teacher = teacher();
        Exam exam = exam(teacher, ExamStatus.BORRADOR, List.of(topic("Tema A",
                question("Tabla", "7TEST_DECISION_TABLE:{\"rows\":3,\"cols\":2,\"cells\":[[\"\",\"\"],[\"\",\"\"] ,[\"\",\"\"]]}", "10"))));
        when(userRepository.findByEmail(teacher.getEmail())).thenReturn(Optional.of(teacher));
        when(examRepository.findById(exam.getId())).thenReturn(Optional.of(exam));

        assertThatThrownBy(() -> examService.publish(teacher.getEmail(), exam.getId()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("respuesta modelo");
    }

    @Test
    void eliminarExamen_borrador_eliminaCorrectamente() {
        User teacher = teacher();
        Exam exam = exam(teacher, ExamStatus.BORRADOR, List.of());
        when(userRepository.findByEmail(teacher.getEmail())).thenReturn(Optional.of(teacher));
        when(examRepository.findById(exam.getId())).thenReturn(Optional.of(exam));

        examService.deleteExam(teacher.getEmail(), exam.getId());

        org.mockito.Mockito.verify(examRepository).deleteById(exam.getId());
    }

    @Test
    void eliminarExamen_cerradoConEntregas_rechaza() {
        User teacher = teacher();
        Exam exam = exam(teacher, ExamStatus.CERRADO, List.of());
        when(userRepository.findByEmail(teacher.getEmail())).thenReturn(Optional.of(teacher));
        when(examRepository.findById(exam.getId())).thenReturn(Optional.of(exam));
        when(submissionRepository.findByExamId(exam.getId())).thenReturn(List.of(
                com.seventest.domain.model.ExamSubmission.builder()
                        .id(UUID.randomUUID())
                        .examId(exam.getId())
                        .studentId(UUID.randomUUID())
                        .status(com.seventest.domain.model.SubmissionStatus.ENTREGADO)
                        .answers(List.of())
                        .startedAt(java.time.Instant.now())
                        .build()
        ));

        assertThatThrownBy(() -> examService.deleteExam(teacher.getEmail(), exam.getId()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("entregas");
    }

    @Test
    void editarPregunta_siTemaSuperaDiez_permiteCambioParaRedistribucion() {
        User teacher = teacher();
        ExamQuestion first = question("P1", "R1", "6");
        ExamQuestion second = question("P2", "R2", "4");
        ExamTopic topic = topic("Tema A", first, second);
        Exam exam = exam(teacher, ExamStatus.BORRADOR, List.of(topic));
        when(userRepository.findByEmail(teacher.getEmail())).thenReturn(Optional.of(teacher));
        when(examRepository.findById(exam.getId())).thenReturn(Optional.of(exam));
        when(examRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Exam result = examService.updateQuestion(
                teacher.getEmail(), exam.getId(), topic.getId(), second.getId(), "P2 editada", "R2 editada", new BigDecimal("5"));

        assertThat(result.getTopics().get(0).totalPoints()).isEqualByComparingTo("11");
    }

    private User teacher() {
        return teacher("profe@test.com");
    }

    private User teacher(String email) {
        return User.builder()
                .id(UUID.randomUUID())
                .fullName("Profesora Test")
                .email(email)
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
