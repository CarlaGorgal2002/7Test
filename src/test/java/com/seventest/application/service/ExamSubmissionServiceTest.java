package com.seventest.application.service;

import com.seventest.domain.exception.ExamNotFoundException;
import com.seventest.domain.exception.ExamSubmissionNotFoundException;
import com.seventest.domain.exception.UserNotFoundException;
import com.seventest.domain.model.Exam;
import com.seventest.domain.model.ExamAnswer;
import com.seventest.domain.model.ExamQuestion;
import com.seventest.domain.model.ExamStatus;
import com.seventest.domain.model.ExamSubmission;
import com.seventest.domain.model.ExamTopic;
import com.seventest.domain.model.Role;
import com.seventest.domain.model.SubmissionStatus;
import com.seventest.domain.model.User;
import com.seventest.domain.model.UserStatus;
import com.seventest.domain.model.IncorrectCorrectionLog;
import com.seventest.domain.port.in.ExamSubmissionUseCase;
import com.seventest.domain.port.out.ExamRepository;
import com.seventest.domain.port.out.ExamSubmissionRepository;
import com.seventest.domain.port.out.UserRepository;
import com.seventest.domain.port.out.IncorrectCorrectionLogRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
class ExamSubmissionServiceTest {

    @Mock
    private ExamSubmissionRepository submissionRepository;

    @Mock
    private ExamRepository examRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private IncorrectCorrectionLogRepository logRepository;

    @InjectMocks
    private ExamSubmissionService examSubmissionService;

    private User createSampleStudent(UUID id, String email) {
        return User.builder()
                .id(id)
                .fullName("Student Joe")
                .email(email)
                .role(Role.ALUMNO)
                .status(UserStatus.ACTIVO)
                .build();
    }

    private User createSampleTeacher(UUID id, String email) {
        return User.builder()
                .id(id)
                .fullName("Teacher Jane")
                .email(email)
                .role(Role.PROFESOR)
                .status(UserStatus.ACTIVO)
                .build();
    }

    private Exam createSampleExam(UUID id, UUID teacherId, ExamStatus status, List<ExamTopic> topics) {
        return Exam.builder()
                .id(id)
                .title("Parcial 1")
                .teacherId(teacherId)
                .status(status)
                .topics(topics)
                .build();
    }

    private ExamTopic createSampleTopic(UUID id, String name, List<ExamQuestion> questions) {
        return ExamTopic.builder()
                .id(id)
                .name(name)
                .questions(questions)
                .build();
    }

    private ExamSubmission createSampleSubmission(UUID id, UUID examId, UUID studentId, SubmissionStatus status, UUID topicId, List<ExamAnswer> answers) {
        return ExamSubmission.builder()
                .id(id)
                .examId(examId)
                .examTitle("Parcial 1")
                .studentId(studentId)
                .status(status)
                .topicId(topicId)
                .answers(answers)
                .build();
    }

    // ========================================== START ==========================================

    @Test
    void start_notStudent_throwsIllegalArgumentException() {
        // Arrange
        UUID teacherId = UUID.randomUUID();
        User teacher = createSampleTeacher(teacherId, "teacher@test.com");
        Mockito.when(userRepository.findByEmail("teacher@test.com")).thenReturn(Optional.of(teacher));

        // Act & Assert
        IllegalArgumentException exception = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> examSubmissionService.start("teacher@test.com", UUID.randomUUID(), UUID.randomUUID())
        );
        Assertions.assertEquals("Solo un alumno puede rendir examenes", exception.getMessage());
    }

    @Test
    void start_examNotFound_throwsExamNotFoundException() {
        // Arrange
        UUID studentId = UUID.randomUUID();
        UUID examId = UUID.randomUUID();
        User student = createSampleStudent(studentId, "student@test.com");
        Mockito.when(userRepository.findByEmail("student@test.com")).thenReturn(Optional.of(student));
        Mockito.when(examRepository.findById(examId)).thenReturn(Optional.empty());

        // Act & Assert
        Assertions.assertThrows(
                ExamNotFoundException.class,
                () -> examSubmissionService.start("student@test.com", examId, UUID.randomUUID())
        );
    }

    @Test
    void start_examNotPublished_throwsIllegalArgumentException() {
        // Arrange
        UUID studentId = UUID.randomUUID();
        UUID examId = UUID.randomUUID();
        User student = createSampleStudent(studentId, "student@test.com");
        Exam exam = createSampleExam(examId, UUID.randomUUID(), ExamStatus.BORRADOR, new ArrayList<>());
        Mockito.when(userRepository.findByEmail("student@test.com")).thenReturn(Optional.of(student));
        Mockito.when(examRepository.findById(examId)).thenReturn(Optional.of(exam));

        // Act & Assert
        IllegalArgumentException exception = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> examSubmissionService.start("student@test.com", examId, UUID.randomUUID())
        );
        Assertions.assertEquals("El examen no esta publicado", exception.getMessage());
    }

    @Test
    void start_availableFromIsInFuture_throwsIllegalArgumentException() {
        // Arrange
        UUID studentId = UUID.randomUUID();
        UUID examId = UUID.randomUUID();
        UUID topicId = UUID.randomUUID();
        User student = createSampleStudent(studentId, "student@test.com");
        ExamTopic topic = createSampleTopic(topicId, "Topic 1", new ArrayList<>());
        Exam exam = createSampleExam(examId, UUID.randomUUID(), ExamStatus.PUBLICADO, List.of(topic)).toBuilder()
                .availableFrom(Instant.now().plusSeconds(3600))
                .build();

        Mockito.when(userRepository.findByEmail("student@test.com")).thenReturn(Optional.of(student));
        Mockito.when(examRepository.findById(examId)).thenReturn(Optional.of(exam));

        // Act & Assert
        IllegalArgumentException exception = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> examSubmissionService.start("student@test.com", examId, topicId)
        );
        Assertions.assertEquals("El examen todavia no esta disponible", exception.getMessage());
    }

    @Test
    void start_topicIdIsNull_throwsIllegalArgumentException() {
        // Arrange
        UUID studentId = UUID.randomUUID();
        UUID examId = UUID.randomUUID();
        User student = createSampleStudent(studentId, "student@test.com");
        Exam exam = createSampleExam(examId, UUID.randomUUID(), ExamStatus.PUBLICADO, new ArrayList<>());
        Mockito.when(userRepository.findByEmail("student@test.com")).thenReturn(Optional.of(student));
        Mockito.when(examRepository.findById(examId)).thenReturn(Optional.of(exam));

        // Act & Assert
        IllegalArgumentException exception = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> examSubmissionService.start("student@test.com", examId, null)
        );
        Assertions.assertEquals("Debes seleccionar un tema para comenzar", exception.getMessage());
    }

    @Test
    void start_topicNotAssignedToExam_throwsIllegalArgumentException() {
        // Arrange
        UUID studentId = UUID.randomUUID();
        UUID examId = UUID.randomUUID();
        UUID topicId = UUID.randomUUID();
        User student = createSampleStudent(studentId, "student@test.com");
        Exam exam = createSampleExam(examId, UUID.randomUUID(), ExamStatus.PUBLICADO, new ArrayList<>());
        Mockito.when(userRepository.findByEmail("student@test.com")).thenReturn(Optional.of(student));
        Mockito.when(examRepository.findById(examId)).thenReturn(Optional.of(exam));

        // Act & Assert
        IllegalArgumentException exception = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> examSubmissionService.start("student@test.com", examId, topicId)
        );
        Assertions.assertEquals("El tema asignado ya no existe", exception.getMessage());
    }

    @Test
    void start_submissionAlreadyExists_returnsExistingSubmission() {
        // Arrange
        UUID studentId = UUID.randomUUID();
        UUID examId = UUID.randomUUID();
        UUID topicId = UUID.randomUUID();
        User student = createSampleStudent(studentId, "student@test.com");
        ExamTopic topic = createSampleTopic(topicId, "Topic 1", new ArrayList<>());
        Exam exam = createSampleExam(examId, UUID.randomUUID(), ExamStatus.PUBLICADO, List.of(topic));
        ExamSubmission existingSubmission = createSampleSubmission(UUID.randomUUID(), examId, studentId, SubmissionStatus.EN_PROGRESO, topicId, new ArrayList<>());

        Mockito.when(userRepository.findByEmail("student@test.com")).thenReturn(Optional.of(student));
        Mockito.when(examRepository.findById(examId)).thenReturn(Optional.of(exam));
        Mockito.when(submissionRepository.findByStudentIdAndExamId(studentId, examId)).thenReturn(Optional.of(existingSubmission));

        // Act
        ExamSubmission result = examSubmissionService.start("student@test.com", examId, topicId);

        // Assert
        Assertions.assertSame(existingSubmission, result);
        Mockito.verify(submissionRepository, Mockito.never()).save(Mockito.any(ExamSubmission.class));
    }

    @Test
    void start_submissionDoesNotExist_createsAndSavesNewSubmission() {
        // Arrange
        UUID studentId = UUID.randomUUID();
        UUID examId = UUID.randomUUID();
        UUID topicId = UUID.randomUUID();
        UUID questionId = UUID.randomUUID();
        User student = createSampleStudent(studentId, "student@test.com");
        ExamQuestion question = ExamQuestion.builder().id(questionId).displayOrder(1).points(BigDecimal.TEN).build();
        ExamTopic topic = createSampleTopic(topicId, "Topic 1", List.of(question));
        Exam exam = createSampleExam(examId, UUID.randomUUID(), ExamStatus.PUBLICADO, List.of(topic));

        Mockito.when(userRepository.findByEmail("student@test.com")).thenReturn(Optional.of(student));
        Mockito.when(examRepository.findById(examId)).thenReturn(Optional.of(exam));
        Mockito.when(submissionRepository.findByStudentIdAndExamId(studentId, examId)).thenReturn(Optional.empty());
        Mockito.when(submissionRepository.save(Mockito.any(ExamSubmission.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        ExamSubmission result = examSubmissionService.start("student@test.com", examId, topicId);

        // Assert
        Assertions.assertNotNull(result);
        Assertions.assertEquals(examId, result.getExamId());
        Assertions.assertEquals(studentId, result.getStudentId());
        Assertions.assertEquals(SubmissionStatus.EN_PROGRESO, result.getStatus());
        Assertions.assertEquals(topicId, result.getTopicId());
        Assertions.assertEquals(1, result.getAnswers().size());
        Assertions.assertEquals(questionId, result.getAnswers().get(0).getQuestionId());
        Mockito.verify(submissionRepository, Mockito.times(1)).save(Mockito.any(ExamSubmission.class));
    }

    // ========================================== SAVE ANSWERS ==========================================

    @Test
    void saveAnswers_notStudent_throwsIllegalArgumentException() {
        // Arrange
        User teacher = createSampleTeacher(UUID.randomUUID(), "teacher@test.com");
        Mockito.when(userRepository.findByEmail("teacher@test.com")).thenReturn(Optional.of(teacher));

        // Act & Assert
        Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> examSubmissionService.saveAnswers("teacher@test.com", UUID.randomUUID(), null)
        );
    }

    @Test
    void saveAnswers_submissionNotFound_throwsExamSubmissionNotFoundException() {
        // Arrange
        UUID submissionId = UUID.randomUUID();
        User student = createSampleStudent(UUID.randomUUID(), "student@test.com");
        Mockito.when(userRepository.findByEmail("student@test.com")).thenReturn(Optional.of(student));
        Mockito.when(submissionRepository.findById(submissionId)).thenReturn(Optional.empty());

        // Act & Assert
        Assertions.assertThrows(
                ExamSubmissionNotFoundException.class,
                () -> examSubmissionService.saveAnswers("student@test.com", submissionId, null)
        );
    }

    @Test
    void saveAnswers_belongsToOtherStudent_throwsIllegalArgumentException() {
        // Arrange
        UUID submissionId = UUID.randomUUID();
        User student = createSampleStudent(UUID.randomUUID(), "student@test.com");
        ExamSubmission submission = createSampleSubmission(submissionId, UUID.randomUUID(), UUID.randomUUID(), SubmissionStatus.EN_PROGRESO, UUID.randomUUID(), new ArrayList<>());
        Mockito.when(userRepository.findByEmail("student@test.com")).thenReturn(Optional.of(student));
        Mockito.when(submissionRepository.findById(submissionId)).thenReturn(Optional.of(submission));

        // Act & Assert
        Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> examSubmissionService.saveAnswers("student@test.com", submissionId, null)
        );
    }

    @Test
    void saveAnswers_statusNotEnProgreso_throwsIllegalArgumentException() {
        // Arrange
        UUID submissionId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        User student = createSampleStudent(studentId, "student@test.com");
        ExamSubmission submission = createSampleSubmission(submissionId, UUID.randomUUID(), studentId, SubmissionStatus.ENTREGADO, UUID.randomUUID(), new ArrayList<>());
        Mockito.when(userRepository.findByEmail("student@test.com")).thenReturn(Optional.of(student));
        Mockito.when(submissionRepository.findById(submissionId)).thenReturn(Optional.of(submission));

        // Act & Assert
        Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> examSubmissionService.saveAnswers("student@test.com", submissionId, null)
        );
    }

    @Test
    void saveAnswers_validInput_updatesAnswersTextAndSaves() {
        // Arrange
        UUID submissionId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        UUID examId = UUID.randomUUID();
        UUID topicId = UUID.randomUUID();
        UUID qId1 = UUID.randomUUID();

        User student = createSampleStudent(studentId, "student@test.com");
        ExamQuestion question = ExamQuestion.builder().id(qId1).prompt("Q1").points(BigDecimal.TEN).build();
        ExamTopic topic = createSampleTopic(topicId, "Topic", List.of(question));
        Exam exam = createSampleExam(examId, UUID.randomUUID(), ExamStatus.PUBLICADO, List.of(topic));

        ExamAnswer answer = ExamAnswer.builder().id(UUID.randomUUID()).questionId(qId1).answerText("Old text").build();
        ExamSubmission submission = createSampleSubmission(submissionId, examId, studentId, SubmissionStatus.EN_PROGRESO, topicId, List.of(answer));

        Mockito.when(userRepository.findByEmail("student@test.com")).thenReturn(Optional.of(student));
        Mockito.when(submissionRepository.findById(submissionId)).thenReturn(Optional.of(submission));
        Mockito.when(examRepository.findById(examId)).thenReturn(Optional.of(exam));
        Mockito.when(submissionRepository.save(Mockito.any(ExamSubmission.class))).thenAnswer(inv -> inv.getArgument(0));

        List<ExamSubmissionUseCase.AnswerUpdate> updates = List.of(
                new ExamSubmissionUseCase.AnswerUpdate(qId1, "   New Text   ")
        );

        // Act
        ExamSubmission result = examSubmissionService.saveAnswers("student@test.com", submissionId, updates);

        // Assert
        Assertions.assertNotNull(result);
        Assertions.assertEquals("New Text", result.getAnswers().get(0).getAnswerText());
        Mockito.verify(submissionRepository, Mockito.times(1)).save(Mockito.any(ExamSubmission.class));
    }

    @Test
    void saveAnswers_withNullUpdates_savesWithNoChanges() {
        // Arrange
        UUID submissionId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        UUID examId = UUID.randomUUID();
        UUID topicId = UUID.randomUUID();
        UUID qId1 = UUID.randomUUID();

        User student = createSampleStudent(studentId, "student@test.com");
        ExamQuestion question = ExamQuestion.builder().id(qId1).prompt("Q1").points(BigDecimal.TEN).build();
        ExamTopic topic = createSampleTopic(topicId, "Topic", List.of(question));
        Exam exam = createSampleExam(examId, UUID.randomUUID(), ExamStatus.PUBLICADO, List.of(topic));

        ExamAnswer answer = ExamAnswer.builder().id(UUID.randomUUID()).questionId(qId1).answerText("Old text").build();
        ExamSubmission submission = createSampleSubmission(submissionId, examId, studentId, SubmissionStatus.EN_PROGRESO, topicId, List.of(answer));

        Mockito.when(userRepository.findByEmail("student@test.com")).thenReturn(Optional.of(student));
        Mockito.when(submissionRepository.findById(submissionId)).thenReturn(Optional.of(submission));
        Mockito.when(examRepository.findById(examId)).thenReturn(Optional.of(exam));
        Mockito.when(submissionRepository.save(Mockito.any(ExamSubmission.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        ExamSubmission result = examSubmissionService.saveAnswers("student@test.com", submissionId, null);

        // Assert
        Assertions.assertNotNull(result);
        Assertions.assertEquals("Old text", result.getAnswers().get(0).getAnswerText());
    }

    @Test
    void saveAnswers_questionDoesNotBelongToTopic_throwsIllegalArgumentException() {
        // Arrange
        UUID submissionId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        UUID examId = UUID.randomUUID();
        UUID topicId = UUID.randomUUID();
        UUID qId1 = UUID.randomUUID();
        UUID invalidQId = UUID.randomUUID();

        User student = createSampleStudent(studentId, "student@test.com");
        ExamQuestion question = ExamQuestion.builder().id(qId1).prompt("Q1").points(BigDecimal.TEN).build();
        ExamTopic topic = createSampleTopic(topicId, "Topic", List.of(question));
        Exam exam = createSampleExam(examId, UUID.randomUUID(), ExamStatus.PUBLICADO, List.of(topic));

        ExamAnswer answer = ExamAnswer.builder().id(UUID.randomUUID()).questionId(qId1).answerText("Old text").build();
        ExamSubmission submission = createSampleSubmission(submissionId, examId, studentId, SubmissionStatus.EN_PROGRESO, topicId, List.of(answer));

        Mockito.when(userRepository.findByEmail("student@test.com")).thenReturn(Optional.of(student));
        Mockito.when(submissionRepository.findById(submissionId)).thenReturn(Optional.of(submission));
        Mockito.when(examRepository.findById(examId)).thenReturn(Optional.of(exam));

        List<ExamSubmissionUseCase.AnswerUpdate> updates = List.of(
                new ExamSubmissionUseCase.AnswerUpdate(invalidQId, "Text")
        );

        // Act & Assert
        Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> examSubmissionService.saveAnswers("student@test.com", submissionId, updates)
        );
    }

    // ========================================== SUBMIT ==========================================

    @Test
    void submit_statusIsEnProgreso_submitsSuccessfully() {
        // Arrange
        UUID submissionId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        User student = createSampleStudent(studentId, "student@test.com");
        ExamSubmission submission = createSampleSubmission(submissionId, UUID.randomUUID(), studentId, SubmissionStatus.EN_PROGRESO, UUID.randomUUID(), new ArrayList<>());

        Mockito.when(userRepository.findByEmail("student@test.com")).thenReturn(Optional.of(student));
        Mockito.when(submissionRepository.findById(submissionId)).thenReturn(Optional.of(submission));
        Mockito.when(submissionRepository.save(Mockito.any(ExamSubmission.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        ExamSubmission result = examSubmissionService.submit("student@test.com", submissionId);

        // Assert
        Assertions.assertNotNull(result);
        Assertions.assertEquals(SubmissionStatus.ENTREGADO, result.getStatus());
        Assertions.assertNotNull(result.getSubmittedAt());
        Mockito.verify(submissionRepository, Mockito.times(1)).save(Mockito.any(ExamSubmission.class));
    }

    @Test
    void submit_statusIsAlreadyEntregado_returnsDirectlyWithoutSaving() {
        // Arrange
        UUID submissionId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        User student = createSampleStudent(studentId, "student@test.com");
        ExamSubmission submission = createSampleSubmission(submissionId, UUID.randomUUID(), studentId, SubmissionStatus.ENTREGADO, UUID.randomUUID(), new ArrayList<>());

        Mockito.when(userRepository.findByEmail("student@test.com")).thenReturn(Optional.of(student));
        Mockito.when(submissionRepository.findById(submissionId)).thenReturn(Optional.of(submission));

        // Act
        ExamSubmission result = examSubmissionService.submit("student@test.com", submissionId);

        // Assert
        Assertions.assertSame(submission, result);
        Mockito.verify(submissionRepository, Mockito.never()).save(Mockito.any(ExamSubmission.class));
    }

    // ========================================== LIST FOR STUDENT ==========================================

    @Test
    void listForStudent_validRequest_returnsStudentSubmissions() {
        // Arrange
        UUID studentId = UUID.randomUUID();
        User student = createSampleStudent(studentId, "student@test.com");
        List<ExamSubmission> expectedList = List.of(createSampleSubmission(UUID.randomUUID(), UUID.randomUUID(), studentId, SubmissionStatus.ENTREGADO, UUID.randomUUID(), new ArrayList<>()));

        Mockito.when(userRepository.findByEmail("student@test.com")).thenReturn(Optional.of(student));
        Mockito.when(submissionRepository.findByStudentId(studentId)).thenReturn(expectedList);

        // Act
        List<ExamSubmission> result = examSubmissionService.listForStudent("student@test.com");

        // Assert
        Assertions.assertEquals(expectedList, result);
    }

    // ========================================== FIND FOR TEACHER ==========================================

    @Test
    void findForTeacher_notTeacher_throwsIllegalArgumentException() {
        // Arrange
        User student = createSampleStudent(UUID.randomUUID(), "student@test.com");
        Mockito.when(userRepository.findByEmail("student@test.com")).thenReturn(Optional.of(student));

        // Act & Assert
        Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> examSubmissionService.findForTeacher("student@test.com", UUID.randomUUID())
        );
    }

    @Test
    void findForTeacher_validTeacherAndExamOwner_returnsSubmission() {
        // Arrange
        UUID teacherId = UUID.randomUUID();
        UUID examId = UUID.randomUUID();
        UUID submissionId = UUID.randomUUID();
        User teacher = createSampleTeacher(teacherId, "teacher@test.com");
        Exam exam = createSampleExam(examId, teacherId, ExamStatus.PUBLICADO, new ArrayList<>());
        ExamSubmission submission = createSampleSubmission(submissionId, examId, UUID.randomUUID(), SubmissionStatus.ENTREGADO, UUID.randomUUID(), new ArrayList<>());

        Mockito.when(userRepository.findByEmail("teacher@test.com")).thenReturn(Optional.of(teacher));
        Mockito.when(submissionRepository.findById(submissionId)).thenReturn(Optional.of(submission));
        Mockito.when(examRepository.findById(examId)).thenReturn(Optional.of(exam));

        // Act
        ExamSubmission result = examSubmissionService.findForTeacher("teacher@test.com", submissionId);

        // Assert
        Assertions.assertSame(submission, result);
    }

    @Test
    void findForTeacher_notExamOwner_throwsIllegalArgumentException() {
        // Arrange
        UUID teacherId = UUID.randomUUID();
        UUID otherTeacherId = UUID.randomUUID();
        UUID examId = UUID.randomUUID();
        UUID submissionId = UUID.randomUUID();
        User teacher = createSampleTeacher(teacherId, "teacher@test.com");
        Exam exam = createSampleExam(examId, otherTeacherId, ExamStatus.PUBLICADO, new ArrayList<>());
        ExamSubmission submission = createSampleSubmission(submissionId, examId, UUID.randomUUID(), SubmissionStatus.ENTREGADO, UUID.randomUUID(), new ArrayList<>());

        Mockito.when(userRepository.findByEmail("teacher@test.com")).thenReturn(Optional.of(teacher));
        Mockito.when(submissionRepository.findById(submissionId)).thenReturn(Optional.of(submission));
        Mockito.when(examRepository.findById(examId)).thenReturn(Optional.of(exam));

        // Act & Assert
        Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> examSubmissionService.findForTeacher("teacher@test.com", submissionId)
        );
    }

    // ========================================== GRADE ==========================================

    @Test
    void grade_validTeacherAndInput_updatesScoresAndSaves() {
        // Arrange
        UUID teacherId = UUID.randomUUID();
        UUID examId = UUID.randomUUID();
        UUID topicId = UUID.randomUUID();
        UUID submissionId = UUID.randomUUID();
        UUID qId1 = UUID.randomUUID();

        User teacher = createSampleTeacher(teacherId, "teacher@test.com");
        ExamQuestion question = ExamQuestion.builder().id(qId1).prompt("Q1").points(new BigDecimal("5.0")).build();
        ExamTopic topic = createSampleTopic(topicId, "Topic", List.of(question));
        Exam exam = createSampleExam(examId, teacherId, ExamStatus.PUBLICADO, List.of(topic));

        ExamAnswer answer = ExamAnswer.builder().id(UUID.randomUUID()).questionId(qId1).answerText("Answer").build();
        ExamSubmission submission = createSampleSubmission(submissionId, examId, UUID.randomUUID(), SubmissionStatus.ENTREGADO, topicId, List.of(answer));

        Mockito.when(userRepository.findByEmail("teacher@test.com")).thenReturn(Optional.of(teacher));
        Mockito.when(submissionRepository.findById(submissionId)).thenReturn(Optional.of(submission));
        Mockito.when(examRepository.findById(examId)).thenReturn(Optional.of(exam));
        Mockito.when(submissionRepository.save(Mockito.any(ExamSubmission.class))).thenAnswer(inv -> inv.getArgument(0));

        List<ExamSubmissionUseCase.GradeUpdate> updates = List.of(
                new ExamSubmissionUseCase.GradeUpdate(qId1, new BigDecimal("4.0"), "   Good job   ")
        );

        // Act
        ExamSubmission result = examSubmissionService.grade("teacher@test.com", submissionId, updates);

        // Assert
        Assertions.assertNotNull(result);
        Assertions.assertEquals(0, new BigDecimal("4.0").compareTo(result.getAnswers().get(0).getScore()));
        Assertions.assertEquals("Good job", result.getAnswers().get(0).getComment());
    }

    @Test
    void grade_withNullUpdates_savesWithNoChanges() {
        // Arrange
        UUID teacherId = UUID.randomUUID();
        UUID examId = UUID.randomUUID();
        UUID topicId = UUID.randomUUID();
        UUID submissionId = UUID.randomUUID();
        UUID qId1 = UUID.randomUUID();

        User teacher = createSampleTeacher(teacherId, "teacher@test.com");
        ExamQuestion question = ExamQuestion.builder().id(qId1).prompt("Q1").points(new BigDecimal("5.0")).build();
        ExamTopic topic = createSampleTopic(topicId, "Topic", List.of(question));
        Exam exam = createSampleExam(examId, teacherId, ExamStatus.PUBLICADO, List.of(topic));

        ExamAnswer answer = ExamAnswer.builder().id(UUID.randomUUID()).questionId(qId1).answerText("Answer").score(new BigDecimal("3.0")).build();
        ExamSubmission submission = createSampleSubmission(submissionId, examId, UUID.randomUUID(), SubmissionStatus.ENTREGADO, topicId, List.of(answer));

        Mockito.when(userRepository.findByEmail("teacher@test.com")).thenReturn(Optional.of(teacher));
        Mockito.when(submissionRepository.findById(submissionId)).thenReturn(Optional.of(submission));
        Mockito.when(examRepository.findById(examId)).thenReturn(Optional.of(exam));
        Mockito.when(submissionRepository.save(Mockito.any(ExamSubmission.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        ExamSubmission result = examSubmissionService.grade("teacher@test.com", submissionId, null);

        // Assert
        Assertions.assertNotNull(result);
        Assertions.assertEquals(0, new BigDecimal("3.0").compareTo(result.getAnswers().get(0).getScore()));
    }

    @Test
    void grade_withNullScoreAndNullComment_updatesCommentToNull() {
        // Arrange
        UUID teacherId = UUID.randomUUID();
        UUID examId = UUID.randomUUID();
        UUID topicId = UUID.randomUUID();
        UUID submissionId = UUID.randomUUID();
        UUID qId1 = UUID.randomUUID();

        User teacher = createSampleTeacher(teacherId, "teacher@test.com");
        ExamQuestion question = ExamQuestion.builder().id(qId1).prompt("Q1").points(new BigDecimal("5.0")).build();
        ExamTopic topic = createSampleTopic(topicId, "Topic", List.of(question));
        Exam exam = createSampleExam(examId, teacherId, ExamStatus.PUBLICADO, List.of(topic));

        ExamAnswer answer = ExamAnswer.builder().id(UUID.randomUUID()).questionId(qId1).answerText("Answer").score(new BigDecimal("3.0")).comment("old comment").build();
        ExamSubmission submission = createSampleSubmission(submissionId, examId, UUID.randomUUID(), SubmissionStatus.ENTREGADO, topicId, List.of(answer));

        Mockito.when(userRepository.findByEmail("teacher@test.com")).thenReturn(Optional.of(teacher));
        Mockito.when(submissionRepository.findById(submissionId)).thenReturn(Optional.of(submission));
        Mockito.when(examRepository.findById(examId)).thenReturn(Optional.of(exam));
        Mockito.when(submissionRepository.save(Mockito.any(ExamSubmission.class))).thenAnswer(inv -> inv.getArgument(0));

        List<ExamSubmissionUseCase.GradeUpdate> updates = List.of(
                new ExamSubmissionUseCase.GradeUpdate(qId1, null, null)
        );

        // Act
        ExamSubmission result = examSubmissionService.grade("teacher@test.com", submissionId, updates);

        // Assert
        Assertions.assertNotNull(result);
        Assertions.assertNull(result.getAnswers().get(0).getScore());
        Assertions.assertNull(result.getAnswers().get(0).getComment());
    }

    @Test
    void grade_negativeScore_throwsIllegalArgumentException() {
        // Arrange
        UUID teacherId = UUID.randomUUID();
        UUID examId = UUID.randomUUID();
        UUID topicId = UUID.randomUUID();
        UUID submissionId = UUID.randomUUID();
        UUID qId1 = UUID.randomUUID();

        User teacher = createSampleTeacher(teacherId, "teacher@test.com");
        ExamQuestion question = ExamQuestion.builder().id(qId1).prompt("Q1").points(new BigDecimal("5.0")).build();
        ExamTopic topic = createSampleTopic(topicId, "Topic", List.of(question));
        Exam exam = createSampleExam(examId, teacherId, ExamStatus.PUBLICADO, List.of(topic));

        ExamAnswer answer = ExamAnswer.builder().id(UUID.randomUUID()).questionId(qId1).answerText("Answer").build();
        ExamSubmission submission = createSampleSubmission(submissionId, examId, UUID.randomUUID(), SubmissionStatus.ENTREGADO, topicId, List.of(answer));

        Mockito.when(userRepository.findByEmail("teacher@test.com")).thenReturn(Optional.of(teacher));
        Mockito.when(submissionRepository.findById(submissionId)).thenReturn(Optional.of(submission));
        Mockito.when(examRepository.findById(examId)).thenReturn(Optional.of(exam));

        List<ExamSubmissionUseCase.GradeUpdate> updates = List.of(
                new ExamSubmissionUseCase.GradeUpdate(qId1, new BigDecimal("-1.0"), "Comment")
        );

        // Act & Assert
        IllegalArgumentException exception = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> examSubmissionService.grade("teacher@test.com", submissionId, updates)
        );
        Assertions.assertEquals("El puntaje no puede ser negativo", exception.getMessage());
    }

    @Test
    void grade_scoreExceedsMaxPoints_throwsIllegalArgumentException() {
        // Arrange
        UUID teacherId = UUID.randomUUID();
        UUID examId = UUID.randomUUID();
        UUID topicId = UUID.randomUUID();
        UUID submissionId = UUID.randomUUID();
        UUID qId1 = UUID.randomUUID();

        User teacher = createSampleTeacher(teacherId, "teacher@test.com");
        ExamQuestion question = ExamQuestion.builder().id(qId1).prompt("Q1").points(new BigDecimal("5.0")).build();
        ExamTopic topic = createSampleTopic(topicId, "Topic", List.of(question));
        Exam exam = createSampleExam(examId, teacherId, ExamStatus.PUBLICADO, List.of(topic));

        ExamAnswer answer = ExamAnswer.builder().id(UUID.randomUUID()).questionId(qId1).answerText("Answer").build();
        ExamSubmission submission = createSampleSubmission(submissionId, examId, UUID.randomUUID(), SubmissionStatus.ENTREGADO, topicId, List.of(answer));

        Mockito.when(userRepository.findByEmail("teacher@test.com")).thenReturn(Optional.of(teacher));
        Mockito.when(submissionRepository.findById(submissionId)).thenReturn(Optional.of(submission));
        Mockito.when(examRepository.findById(examId)).thenReturn(Optional.of(exam));

        List<ExamSubmissionUseCase.GradeUpdate> updates = List.of(
                new ExamSubmissionUseCase.GradeUpdate(qId1, new BigDecimal("5.1"), "Comment")
        );

        // Act & Assert
        IllegalArgumentException exception = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> examSubmissionService.grade("teacher@test.com", submissionId, updates)
        );
        Assertions.assertEquals("El puntaje no puede superar el valor máximo de la pregunta", exception.getMessage());
    }

    // ========================================== LIST FOR TEACHER EXAM ==========================================

    @Test
    void listForTeacherExam_validRequest_returnsSubmissions() {
        // Arrange
        UUID teacherId = UUID.randomUUID();
        UUID examId = UUID.randomUUID();
        User teacher = createSampleTeacher(teacherId, "teacher@test.com");
        Exam exam = createSampleExam(examId, teacherId, ExamStatus.PUBLICADO, new ArrayList<>());
        List<ExamSubmission> expectedSubmissions = List.of(createSampleSubmission(UUID.randomUUID(), examId, UUID.randomUUID(), SubmissionStatus.ENTREGADO, UUID.randomUUID(), new ArrayList<>()));

        Mockito.when(userRepository.findByEmail("teacher@test.com")).thenReturn(Optional.of(teacher));
        Mockito.when(examRepository.findById(examId)).thenReturn(Optional.of(exam));
        Mockito.when(submissionRepository.findByExamId(examId)).thenReturn(expectedSubmissions);

        // Act
        List<ExamSubmission> result = examSubmissionService.listForTeacherExam("teacher@test.com", examId);

        // Assert
        Assertions.assertEquals(expectedSubmissions, result);
    }

    @Test
    void start_studentNotFound_throwsUserNotFoundException() {
        // Arrange
        Mockito.when(userRepository.findByEmail("nonexistent@test.com")).thenReturn(Optional.empty());

        // Act & Assert
        Assertions.assertThrows(
                UserNotFoundException.class,
                () -> examSubmissionService.start("nonexistent@test.com", UUID.randomUUID(), UUID.randomUUID())
        );
    }

    @Test
    void grade_withDuplicateGradeUpdates_usesLastUpdate() {
        // Arrange
        UUID teacherId = UUID.randomUUID();
        UUID examId = UUID.randomUUID();
        UUID topicId = UUID.randomUUID();
        UUID submissionId = UUID.randomUUID();
        UUID qId = UUID.randomUUID();

        User teacher = createSampleTeacher(teacherId, "teacher@test.com");
        ExamQuestion question = ExamQuestion.builder().id(qId).prompt("Q").points(BigDecimal.TEN).build();
        ExamTopic topic = createSampleTopic(topicId, "Topic", List.of(question));
        Exam exam = createSampleExam(examId, teacherId, ExamStatus.PUBLICADO, List.of(topic));
        ExamAnswer answer = ExamAnswer.builder().questionId(qId).build();
        ExamSubmission submission = createSampleSubmission(submissionId, examId, UUID.randomUUID(), SubmissionStatus.ENTREGADO, topicId, List.of(answer));

        Mockito.when(userRepository.findByEmail("teacher@test.com")).thenReturn(Optional.of(teacher));
        Mockito.when(submissionRepository.findById(submissionId)).thenReturn(Optional.of(submission));
        Mockito.when(examRepository.findById(examId)).thenReturn(Optional.of(exam));
        Mockito.when(submissionRepository.save(Mockito.any(ExamSubmission.class))).thenAnswer(inv -> inv.getArgument(0));

        List<ExamSubmissionUseCase.GradeUpdate> updates = List.of(
                new ExamSubmissionUseCase.GradeUpdate(qId, new BigDecimal("4.0"), "First"),
                new ExamSubmissionUseCase.GradeUpdate(qId, new BigDecimal("5.0"), "Last")
        );

        // Act
        ExamSubmission result = examSubmissionService.grade("teacher@test.com", submissionId, updates);

        // Assert
        Assertions.assertNotNull(result);
        Assertions.assertEquals(0, new BigDecimal("5.0").compareTo(result.getAnswers().get(0).getScore()));
        Assertions.assertEquals("Last", result.getAnswers().get(0).getComment());
    }

    @Test
    void saveAnswers_withDuplicateAnswerUpdates_usesLastUpdate() {
        // Arrange
        UUID studentId = UUID.randomUUID();
        UUID examId = UUID.randomUUID();
        UUID topicId = UUID.randomUUID();
        UUID submissionId = UUID.randomUUID();
        UUID qId = UUID.randomUUID();

        User student = createSampleStudent(studentId, "student@test.com");
        ExamQuestion question = ExamQuestion.builder().id(qId).prompt("Q").points(BigDecimal.TEN).build();
        ExamTopic topic = createSampleTopic(topicId, "Topic", List.of(question));
        Exam exam = createSampleExam(examId, UUID.randomUUID(), ExamStatus.PUBLICADO, List.of(topic));
        ExamAnswer answer = ExamAnswer.builder().questionId(qId).answerText("").build();
        ExamSubmission submission = createSampleSubmission(submissionId, examId, studentId, SubmissionStatus.EN_PROGRESO, topicId, List.of(answer));

        Mockito.when(userRepository.findByEmail("student@test.com")).thenReturn(Optional.of(student));
        Mockito.when(submissionRepository.findById(submissionId)).thenReturn(Optional.of(submission));
        Mockito.when(examRepository.findById(examId)).thenReturn(Optional.of(exam));
        Mockito.when(submissionRepository.save(Mockito.any(ExamSubmission.class))).thenAnswer(inv -> inv.getArgument(0));

        List<ExamSubmissionUseCase.AnswerUpdate> updates = List.of(
                new ExamSubmissionUseCase.AnswerUpdate(qId, "First answer"),
                new ExamSubmissionUseCase.AnswerUpdate(qId, "Last answer")
        );

        // Act
        ExamSubmission result = examSubmissionService.saveAnswers("student@test.com", submissionId, updates);

        // Assert
        Assertions.assertNotNull(result);
        Assertions.assertEquals("Last answer", result.getAnswers().get(0).getAnswerText());
    }

    @Test
    void saveAnswers_withNullAnswerText_savesAsEmptyString() {
        // Arrange
        UUID studentId = UUID.randomUUID();
        UUID examId = UUID.randomUUID();
        UUID topicId = UUID.randomUUID();
        UUID submissionId = UUID.randomUUID();
        UUID qId = UUID.randomUUID();

        User student = createSampleStudent(studentId, "student@test.com");
        ExamQuestion question = ExamQuestion.builder().id(qId).prompt("Q").points(BigDecimal.TEN).build();
        ExamTopic topic = createSampleTopic(topicId, "Topic", List.of(question));
        Exam exam = createSampleExam(examId, UUID.randomUUID(), ExamStatus.PUBLICADO, List.of(topic));
        ExamAnswer answer = ExamAnswer.builder().questionId(qId).answerText("Old").build();
        ExamSubmission submission = createSampleSubmission(submissionId, examId, studentId, SubmissionStatus.EN_PROGRESO, topicId, List.of(answer));

        Mockito.when(userRepository.findByEmail("student@test.com")).thenReturn(Optional.of(student));
        Mockito.when(submissionRepository.findById(submissionId)).thenReturn(Optional.of(submission));
        Mockito.when(examRepository.findById(examId)).thenReturn(Optional.of(exam));
        Mockito.when(submissionRepository.save(Mockito.any(ExamSubmission.class))).thenAnswer(inv -> inv.getArgument(0));

        List<ExamSubmissionUseCase.AnswerUpdate> updates = List.of(
                new ExamSubmissionUseCase.AnswerUpdate(qId, null)
        );

        // Act
        ExamSubmission result = examSubmissionService.saveAnswers("student@test.com", submissionId, updates);

        // Assert
        Assertions.assertNotNull(result);
        Assertions.assertEquals("", result.getAnswers().get(0).getAnswerText());
    }

    @Test
    void start_availableFromIsInPast_succeeds() {
        // Arrange
        UUID studentId = UUID.randomUUID();
        UUID examId = UUID.randomUUID();
        UUID topicId = UUID.randomUUID();
        User student = createSampleStudent(studentId, "student@test.com");
        ExamTopic topic = createSampleTopic(topicId, "Topic 1", new ArrayList<>());
        Exam exam = createSampleExam(examId, UUID.randomUUID(), ExamStatus.PUBLICADO, List.of(topic)).toBuilder()
                .availableFrom(Instant.now().minusSeconds(3600))
                .build();

        Mockito.when(userRepository.findByEmail("student@test.com")).thenReturn(Optional.of(student));
        Mockito.when(examRepository.findById(examId)).thenReturn(Optional.of(exam));
        Mockito.when(submissionRepository.findByStudentIdAndExamId(studentId, examId)).thenReturn(Optional.empty());
        Mockito.when(submissionRepository.save(Mockito.any(ExamSubmission.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        ExamSubmission result = examSubmissionService.start("student@test.com", examId, topicId);

        // Assert
        Assertions.assertNotNull(result);
        Assertions.assertEquals(examId, result.getExamId());
    }

    @Test
    void findForTeacher_submissionNotFound_throwsExamSubmissionNotFoundException() {
        // Arrange
        UUID submissionId = UUID.randomUUID();
        User teacher = createSampleTeacher(UUID.randomUUID(), "teacher@test.com");
        Mockito.when(userRepository.findByEmail("teacher@test.com")).thenReturn(Optional.of(teacher));
        Mockito.when(submissionRepository.findById(submissionId)).thenReturn(Optional.empty());

        // Act & Assert
        Assertions.assertThrows(
                ExamSubmissionNotFoundException.class,
                () -> examSubmissionService.findForTeacher("teacher@test.com", submissionId)
        );
    }

    @Test
    void grade_submissionNotFound_throwsExamSubmissionNotFoundException() {
        // Arrange
        UUID submissionId = UUID.randomUUID();
        User teacher = createSampleTeacher(UUID.randomUUID(), "teacher@test.com");
        Mockito.when(userRepository.findByEmail("teacher@test.com")).thenReturn(Optional.of(teacher));
        Mockito.when(submissionRepository.findById(submissionId)).thenReturn(Optional.empty());

        // Act & Assert
        Assertions.assertThrows(
                ExamSubmissionNotFoundException.class,
                () -> examSubmissionService.grade("teacher@test.com", submissionId, new ArrayList<>())
        );
    }

    @Test
    void listForTeacherExam_notExamOwner_throwsIllegalArgumentException() {
        // Arrange
        UUID teacherId = UUID.randomUUID();
        UUID otherTeacherId = UUID.randomUUID();
        UUID examId = UUID.randomUUID();
        User teacher = createSampleTeacher(teacherId, "teacher@test.com");
        Exam exam = createSampleExam(examId, otherTeacherId, ExamStatus.PUBLICADO, new ArrayList<>());

        Mockito.when(userRepository.findByEmail("teacher@test.com")).thenReturn(Optional.of(teacher));
        Mockito.when(examRepository.findById(examId)).thenReturn(Optional.of(exam));

        // Act & Assert
        IllegalArgumentException exception = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> examSubmissionService.listForTeacherExam("teacher@test.com", examId)
        );
        Assertions.assertEquals("El examen pertenece a otro profesor", exception.getMessage());
    }

    @Test
    void grade_questionIdNotInMaxPoints_succeedsWithoutMaxPointsCheck() {
        // Arrange
        UUID teacherId = UUID.randomUUID();
        UUID examId = UUID.randomUUID();
        UUID topicId = UUID.randomUUID();
        UUID submissionId = UUID.randomUUID();
        UUID unknownQuestionId = UUID.randomUUID();

        User teacher = createSampleTeacher(teacherId, "teacher@test.com");
        ExamTopic topic = createSampleTopic(topicId, "Topic", new ArrayList<>());
        Exam exam = createSampleExam(examId, teacherId, ExamStatus.PUBLICADO, List.of(topic));

        ExamAnswer answer = ExamAnswer.builder().id(UUID.randomUUID()).questionId(unknownQuestionId).answerText("Answer").build();
        ExamSubmission submission = createSampleSubmission(submissionId, examId, UUID.randomUUID(), SubmissionStatus.ENTREGADO, topicId, List.of(answer));

        Mockito.when(userRepository.findByEmail("teacher@test.com")).thenReturn(Optional.of(teacher));
        Mockito.when(submissionRepository.findById(submissionId)).thenReturn(Optional.of(submission));
        Mockito.when(examRepository.findById(examId)).thenReturn(Optional.of(exam));
        Mockito.when(submissionRepository.save(Mockito.any(ExamSubmission.class))).thenAnswer(inv -> inv.getArgument(0));

        List<ExamSubmissionUseCase.GradeUpdate> updates = List.of(
                new ExamSubmissionUseCase.GradeUpdate(unknownQuestionId, new BigDecimal("8.5"), "Good")
        );

        // Act
        ExamSubmission result = examSubmissionService.grade("teacher@test.com", submissionId, updates);

        // Assert
        Assertions.assertNotNull(result);
        Assertions.assertEquals(0, new BigDecimal("8.5").compareTo(result.getAnswers().get(0).getScore()));
        Assertions.assertEquals("Good", result.getAnswers().get(0).getComment());
    }

    @Test
    void grade_notExamOwner_throwsIllegalArgumentException() {
        // Arrange
        UUID teacherId = UUID.randomUUID();
        UUID otherTeacherId = UUID.randomUUID();
        UUID examId = UUID.randomUUID();
        UUID submissionId = UUID.randomUUID();
        User teacher = createSampleTeacher(teacherId, "teacher@test.com");
        Exam exam = createSampleExam(examId, otherTeacherId, ExamStatus.PUBLICADO, new ArrayList<>());
        ExamSubmission submission = createSampleSubmission(submissionId, examId, UUID.randomUUID(), SubmissionStatus.ENTREGADO, UUID.randomUUID(), new ArrayList<>());

        Mockito.when(userRepository.findByEmail("teacher@test.com")).thenReturn(Optional.of(teacher));
        Mockito.when(submissionRepository.findById(submissionId)).thenReturn(Optional.of(submission));
        Mockito.when(examRepository.findById(examId)).thenReturn(Optional.of(exam));

        // Act & Assert
        IllegalArgumentException exception = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> examSubmissionService.grade("teacher@test.com", submissionId, new ArrayList<>())
        );
        Assertions.assertEquals("El examen pertenece a otro profesor", exception.getMessage());
    }

    @Test
    void grade_withDiscrepancyLog_savesLogAndRecalculatesFinalScore() {
        // Arrange
        UUID teacherId = UUID.randomUUID();
        UUID examId = UUID.randomUUID();
        UUID topicId = UUID.randomUUID();
        UUID submissionId = UUID.randomUUID();
        UUID qId1 = UUID.randomUUID();
        UUID qId2 = UUID.randomUUID();

        User teacher = createSampleTeacher(teacherId, "teacher@test.com");
        ExamQuestion q1 = ExamQuestion.builder().id(qId1).prompt("Q1").points(new BigDecimal("5.0")).build();
        ExamQuestion q2 = ExamQuestion.builder().id(qId2).prompt("Q2").points(new BigDecimal("5.0")).build();
        ExamTopic topic = createSampleTopic(topicId, "Topic", List.of(q1, q2));
        Exam exam = createSampleExam(examId, teacherId, ExamStatus.PUBLICADO, List.of(topic));

        ExamAnswer a1 = ExamAnswer.builder()
                .id(UUID.randomUUID())
                .questionId(qId1)
                .answerText("Answer 1")
                .scoreIa(new BigDecimal("3.0"))
                .accuracyIa(new BigDecimal("0.6"))
                .build();
        ExamAnswer a2 = ExamAnswer.builder()
                .id(UUID.randomUUID())
                .questionId(qId2)
                .answerText("Answer 2")
                .scoreIa(new BigDecimal("4.0"))
                .accuracyIa(new BigDecimal("0.8"))
                .build();
        ExamSubmission submission = createSampleSubmission(submissionId, examId, UUID.randomUUID(), SubmissionStatus.ENTREGADO, topicId, List.of(a1, a2));

        Mockito.when(userRepository.findByEmail("teacher@test.com")).thenReturn(Optional.of(teacher));
        Mockito.when(submissionRepository.findById(submissionId)).thenReturn(Optional.of(submission));
        Mockito.when(examRepository.findById(examId)).thenReturn(Optional.of(exam));
        Mockito.when(submissionRepository.save(Mockito.any(ExamSubmission.class))).thenAnswer(inv -> inv.getArgument(0));

        // update q1 with different score and correctionIncorrect = true
        // update q2 with same score
        List<ExamSubmissionUseCase.GradeUpdate> updates = List.of(
                new ExamSubmissionUseCase.GradeUpdate(qId1, new BigDecimal("4.5"), "Updated by teacher", true),
                new ExamSubmissionUseCase.GradeUpdate(qId2, new BigDecimal("4.0"), "IA score correct", false)
        );

        // Act
        ExamSubmission result = examSubmissionService.grade("teacher@test.com", submissionId, updates);

        // Assert
        Assertions.assertNotNull(result);
        Assertions.assertEquals(0, new BigDecimal("4.5").compareTo(result.getAnswers().get(0).getScore()));
        Assertions.assertEquals(0, new BigDecimal("4.0").compareTo(result.getAnswers().get(1).getScore()));
        // check final score is the sum: 4.5 + 4.0 = 8.5
        Assertions.assertEquals(0, new BigDecimal("8.5").compareTo(result.getFinalScore()));
        // all questions scored, so reviewed should be true
        Assertions.assertTrue(result.isReviewed());

        // verify log repository was called once for the discrepancy
        Mockito.verify(logRepository, Mockito.times(1)).save(Mockito.any(IncorrectCorrectionLog.class));
    }
}
