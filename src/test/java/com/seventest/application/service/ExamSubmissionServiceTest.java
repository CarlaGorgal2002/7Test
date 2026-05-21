package com.seventest.application.service;

import com.seventest.domain.model.Exam;
import com.seventest.domain.model.ExamQuestion;
import com.seventest.domain.model.ExamStatus;
import com.seventest.domain.model.ExamSubmission;
import com.seventest.domain.model.ExamTopic;
import com.seventest.domain.model.Role;
import com.seventest.domain.model.SubmissionStatus;
import com.seventest.domain.model.User;
import com.seventest.domain.model.UserStatus;
import com.seventest.domain.port.in.ExamSubmissionUseCase;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExamSubmissionServiceTest {

    @Mock ExamSubmissionRepository submissionRepository;
    @Mock ExamRepository examRepository;
    @Mock UserRepository userRepository;

    @InjectMocks ExamSubmissionService submissionService;

    @Test
    void iniciarExamenPublicado_creaEntregaEnProgresoConTemaAsignado() {
        User student = user(Role.ALUMNO);
        Exam exam = publishedExam();
        when(userRepository.findByEmail(student.getEmail())).thenReturn(Optional.of(student));
        when(examRepository.findById(exam.getId())).thenReturn(Optional.of(exam));
        when(submissionRepository.findByStudentIdAndExamId(student.getId(), exam.getId())).thenReturn(Optional.empty());
        when(submissionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ExamSubmission result = submissionService.start(student.getEmail(), exam.getId());

        assertThat(result.getStatus()).isEqualTo(SubmissionStatus.EN_PROGRESO);
        assertThat(result.getStudentId()).isEqualTo(student.getId());
        assertThat(result.getExamId()).isEqualTo(exam.getId());
        assertThat(result.getAnswers()).hasSize(1);
        assertThat(result.getAnswers().getFirst().getAnswerText()).isEmpty();
    }

    @Test
    void guardarRespuestas_deEntregaEnProgreso_actualizaTexto() {
        User student = user(Role.ALUMNO);
        Exam exam = publishedExam();
        ExamSubmission submission = submission(student, exam, SubmissionStatus.EN_PROGRESO, "");
        UUID questionId = exam.getTopics().getFirst().getQuestions().getFirst().getId();
        when(userRepository.findByEmail(student.getEmail())).thenReturn(Optional.of(student));
        when(submissionRepository.findById(submission.getId())).thenReturn(Optional.of(submission));
        when(examRepository.findById(exam.getId())).thenReturn(Optional.of(exam));
        when(submissionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ExamSubmission result = submissionService.saveAnswers(student.getEmail(), submission.getId(),
                List.of(new ExamSubmissionUseCase.AnswerUpdate(questionId, "Mi respuesta")));

        assertThat(result.getAnswers().getFirst().getAnswerText()).isEqualTo("Mi respuesta");
    }

    @Test
    void guardarRespuestas_deEntregaFinalizada_rechazaCambio() {
        User student = user(Role.ALUMNO);
        Exam exam = publishedExam();
        ExamSubmission submission = submission(student, exam, SubmissionStatus.ENTREGADO, "final");
        UUID questionId = exam.getTopics().getFirst().getQuestions().getFirst().getId();
        when(userRepository.findByEmail(student.getEmail())).thenReturn(Optional.of(student));
        when(submissionRepository.findById(submission.getId())).thenReturn(Optional.of(submission));

        assertThatThrownBy(() -> submissionService.saveAnswers(student.getEmail(), submission.getId(),
                List.of(new ExamSubmissionUseCase.AnswerUpdate(questionId, "Cambio"))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("finalizada");
    }

    @Test
    void entregarExamen_cambiaEstadoAEntregado() {
        User student = user(Role.ALUMNO);
        Exam exam = publishedExam();
        ExamSubmission submission = submission(student, exam, SubmissionStatus.EN_PROGRESO, "respuesta");
        when(userRepository.findByEmail(student.getEmail())).thenReturn(Optional.of(student));
        when(submissionRepository.findById(submission.getId())).thenReturn(Optional.of(submission));
        when(submissionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ExamSubmission result = submissionService.submit(student.getEmail(), submission.getId());

        assertThat(result.getStatus()).isEqualTo(SubmissionStatus.ENTREGADO);
        assertThat(result.getSubmittedAt()).isNotNull();
    }

    private User user(Role role) {
        return User.builder()
                .id(UUID.randomUUID())
                .fullName(role == Role.ALUMNO ? "Alumno Test" : "Profesor Test")
                .email(role.name().toLowerCase() + "@test.com")
                .role(role)
                .status(UserStatus.ACTIVO)
                .passwordHash("hash")
                .failedLoginAttempts(0)
                .lockedUntil(null)
                .build();
    }

    private Exam publishedExam() {
        ExamQuestion question = ExamQuestion.builder()
                .id(UUID.randomUUID())
                .prompt("Definir caja negra")
                .modelAnswer("Tecnica funcional")
                .points(BigDecimal.TEN)
                .displayOrder(1)
                .build();
        ExamTopic topic = ExamTopic.builder()
                .id(UUID.randomUUID())
                .name("Tema A")
                .questions(List.of(question))
                .build();
        Instant now = Instant.now();
        return Exam.builder()
                .id(UUID.randomUUID())
                .title("Parcial")
                .description("")
                .teacherId(UUID.randomUUID())
                .teacherName("Profesor")
                .status(ExamStatus.PUBLICADO)
                .durationMinutes(120)
                .topics(List.of(topic))
                .createdAt(now)
                .updatedAt(now)
                .publishedAt(now)
                .build();
    }

    private ExamSubmission submission(User student, Exam exam, SubmissionStatus status, String answerText) {
        ExamQuestion question = exam.getTopics().getFirst().getQuestions().getFirst();
        Instant now = Instant.now();
        return ExamSubmission.builder()
                .id(UUID.randomUUID())
                .examId(exam.getId())
                .examTitle(exam.getTitle())
                .topicId(exam.getTopics().getFirst().getId())
                .topicName(exam.getTopics().getFirst().getName())
                .studentId(student.getId())
                .studentName(student.getFullName())
                .status(status)
                .answers(List.of(com.seventest.domain.model.ExamAnswer.builder()
                        .id(UUID.randomUUID())
                        .questionId(question.getId())
                        .answerText(answerText)
                        .updatedAt(now)
                        .build()))
                .startedAt(now)
                .updatedAt(now)
                .submittedAt(status == SubmissionStatus.ENTREGADO ? now : null)
                .build();
    }
}
