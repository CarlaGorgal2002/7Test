package com.seventest.application.service;

import com.seventest.domain.exception.ExamNotFoundException;
import com.seventest.domain.exception.UserNotFoundException;
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
class ExamServiceTest {

    @Mock
    private ExamRepository examRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ExamSubmissionRepository submissionRepository;

    @InjectMocks
    private ExamService examService;

    private User createSampleTeacher(UUID id, String email) {
        return User.builder()
                .id(id)
                .fullName("Teacher Jane")
                .email(email)
                .role(Role.PROFESOR)
                .status(UserStatus.ACTIVO)
                .build();
    }

    private User createSampleStudent(UUID id, String email) {
        return User.builder()
                .id(id)
                .fullName("Student Joe")
                .email(email)
                .role(Role.ALUMNO)
                .status(UserStatus.ACTIVO)
                .build();
    }

    private Exam createSampleExam(UUID id, UUID teacherId, ExamStatus status, List<ExamTopic> topics) {
        return Exam.builder()
                .id(id)
                .title("Parcial 1")
                .description("Desc")
                .courseName("Testing")
                .teacherId(teacherId)
                .teacherName("Teacher Jane")
                .status(status)
                .durationMinutes(60)
                .topics(topics)
                .build();
    }

    private ExamTopic createSampleTopic(UUID id, String name, List<ExamQuestion> questions) {
        return ExamTopic.builder()
                .id(id)
                .name(name)
                .colorHex("#2563EB")
                .questions(questions)
                .build();
    }

    private ExamQuestion createSampleQuestion(UUID id, String prompt, String modelAnswer, BigDecimal points, int order) {
        return ExamQuestion.builder()
                .id(id)
                .prompt(prompt)
                .modelAnswer(modelAnswer)
                .points(points)
                .displayOrder(order)
                .build();
    }

    // ========================================== CREATE ==========================================

    @Test
    void create_withValidData_savesAndReturnsExam() {
        // Arrange
        UUID teacherId = UUID.randomUUID();
        User teacher = createSampleTeacher(teacherId, "teacher@test.com");
        Mockito.when(userRepository.findByEmail("teacher@test.com")).thenReturn(Optional.of(teacher));
        Mockito.when(examRepository.save(Mockito.any(Exam.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        Exam result = examService.create("teacher@test.com", "Parcial 1", "Sujeto a cambios", "Testing", Instant.now(), 90);

        // Assert
        Assertions.assertNotNull(result);
        Assertions.assertEquals("Parcial 1", result.getTitle());
        Assertions.assertEquals("Sujeto a cambios", result.getDescription());
        Assertions.assertEquals("Testing", result.getCourseName());
        Assertions.assertEquals(ExamStatus.BORRADOR, result.getStatus());
        Assertions.assertEquals(teacherId, result.getTeacherId());
        Mockito.verify(examRepository, Mockito.times(1)).save(Mockito.any(Exam.class));
    }

    @Test
    void create_withNullCourseName_usesDefaultCourseName() {
        // Arrange
        UUID teacherId = UUID.randomUUID();
        User teacher = createSampleTeacher(teacherId, "teacher@test.com");
        Mockito.when(userRepository.findByEmail("teacher@test.com")).thenReturn(Optional.of(teacher));
        Mockito.when(examRepository.save(Mockito.any(Exam.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        Exam result = examService.create("teacher@test.com", "Parcial 1", null, "   ", Instant.now(), 90);

        // Assert
        Assertions.assertNotNull(result);
        Assertions.assertEquals("Testing de Aplicaciones", result.getCourseName());
    }

    @Test
    void create_userNotExists_throwsUserNotFoundException() {
        // Arrange
        Mockito.when(userRepository.findByEmail("nonexistent@test.com")).thenReturn(Optional.empty());

        // Act & Assert
        Assertions.assertThrows(
                UserNotFoundException.class,
                () -> examService.create("nonexistent@test.com", "P1", "D", "C", null, 60)
        );
    }

    @Test
    void create_userNotTeacher_throwsIllegalArgumentException() {
        // Arrange
        User student = createSampleStudent(UUID.randomUUID(), "student@test.com");
        Mockito.when(userRepository.findByEmail("student@test.com")).thenReturn(Optional.of(student));

        // Act & Assert
        IllegalArgumentException exception = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> examService.create("student@test.com", "P1", "D", "C", null, 60)
        );
        Assertions.assertEquals("Solo un profesor puede gestionar examenes", exception.getMessage());
    }

    @Test
    void create_emptyTitle_throwsIllegalArgumentException() {
        // Arrange
        User teacher = createSampleTeacher(UUID.randomUUID(), "teacher@test.com");
        Mockito.when(userRepository.findByEmail("teacher@test.com")).thenReturn(Optional.of(teacher));

        // Act & Assert
        Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> examService.create("teacher@test.com", "   ", "D", "C", null, 60)
        );
    }

    // ========================================== UPDATE ==========================================

    @Test
    void update_validExamAndTeacher_updatesAndSavesExam() {
        // Arrange
        UUID teacherId = UUID.randomUUID();
        UUID examId = UUID.randomUUID();
        User teacher = createSampleTeacher(teacherId, "teacher@test.com");
        Exam exam = createSampleExam(examId, teacherId, ExamStatus.BORRADOR, new ArrayList<>());
        Mockito.when(userRepository.findByEmail("teacher@test.com")).thenReturn(Optional.of(teacher));
        Mockito.when(examRepository.findById(examId)).thenReturn(Optional.of(exam));
        Mockito.when(examRepository.save(Mockito.any(Exam.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        Exam result = examService.update("teacher@test.com", examId, "New Title", "New Desc", "New Course", null, 120);

        // Assert
        Assertions.assertNotNull(result);
        Assertions.assertEquals("New Title", result.getTitle());
        Assertions.assertEquals("New Desc", result.getDescription());
        Assertions.assertEquals("New Course", result.getCourseName());
        Assertions.assertEquals(120, result.getDurationMinutes());
    }

    @Test
    void update_notOwnedExam_throwsIllegalArgumentException() {
        // Arrange
        UUID teacherId = UUID.randomUUID();
        UUID otherTeacherId = UUID.randomUUID();
        UUID examId = UUID.randomUUID();
        User teacher = createSampleTeacher(teacherId, "teacher@test.com");
        Exam exam = createSampleExam(examId, otherTeacherId, ExamStatus.BORRADOR, new ArrayList<>());
        Mockito.when(userRepository.findByEmail("teacher@test.com")).thenReturn(Optional.of(teacher));
        Mockito.when(examRepository.findById(examId)).thenReturn(Optional.of(exam));

        // Act & Assert
        IllegalArgumentException exception = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> examService.update("teacher@test.com", examId, "New Title", "New Desc", "New Course", null, 120)
        );
        Assertions.assertEquals("El examen pertenece a otro profesor", exception.getMessage());
    }

    @Test
    void update_notInBorradorStatus_throwsIllegalArgumentException() {
        // Arrange
        UUID teacherId = UUID.randomUUID();
        UUID examId = UUID.randomUUID();
        User teacher = createSampleTeacher(teacherId, "teacher@test.com");
        Exam exam = createSampleExam(examId, teacherId, ExamStatus.PUBLICADO, new ArrayList<>());
        Mockito.when(userRepository.findByEmail("teacher@test.com")).thenReturn(Optional.of(teacher));
        Mockito.when(examRepository.findById(examId)).thenReturn(Optional.of(exam));

        // Act & Assert
        IllegalArgumentException exception = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> examService.update("teacher@test.com", examId, "New Title", "New Desc", "New Course", null, 120)
        );
        Assertions.assertEquals("Solo se puede editar un examen en borrador", exception.getMessage());
    }

    // ========================================== TOPICS ==========================================

    @Test
    void addTopic_validInput_addsAndSaves() {
        // Arrange
        UUID teacherId = UUID.randomUUID();
        UUID examId = UUID.randomUUID();
        User teacher = createSampleTeacher(teacherId, "teacher@test.com");
        Exam exam = createSampleExam(examId, teacherId, ExamStatus.BORRADOR, new ArrayList<>());
        Mockito.when(userRepository.findByEmail("teacher@test.com")).thenReturn(Optional.of(teacher));
        Mockito.when(examRepository.findById(examId)).thenReturn(Optional.of(exam));
        Mockito.when(examRepository.save(Mockito.any(Exam.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        Exam result = examService.addTopic("teacher@test.com", examId, "Tema 1");

        // Assert
        Assertions.assertNotNull(result);
        Assertions.assertEquals(1, result.getTopics().size());
        Assertions.assertEquals("Tema 1", result.getTopics().get(0).getName());
        Assertions.assertEquals("#1956D8", result.getTopics().get(0).getColorHex());
    }

    @Test
    void updateTopic_validInput_updatesTopicName() {
        // Arrange
        UUID teacherId = UUID.randomUUID();
        UUID examId = UUID.randomUUID();
        UUID topicId = UUID.randomUUID();
        User teacher = createSampleTeacher(teacherId, "teacher@test.com");
        ExamTopic topic = createSampleTopic(topicId, "Old Name", new ArrayList<>());
        Exam exam = createSampleExam(examId, teacherId, ExamStatus.BORRADOR, List.of(topic));
        Mockito.when(userRepository.findByEmail("teacher@test.com")).thenReturn(Optional.of(teacher));
        Mockito.when(examRepository.findById(examId)).thenReturn(Optional.of(exam));
        Mockito.when(examRepository.save(Mockito.any(Exam.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        Exam result = examService.updateTopic("teacher@test.com", examId, topicId, "New Name");

        // Assert
        Assertions.assertNotNull(result);
        Assertions.assertEquals("New Name", result.getTopics().get(0).getName());
    }

    @Test
    void updateTopic_topicNotFound_throwsIllegalArgumentException() {
        // Arrange
        UUID teacherId = UUID.randomUUID();
        UUID examId = UUID.randomUUID();
        User teacher = createSampleTeacher(teacherId, "teacher@test.com");
        Exam exam = createSampleExam(examId, teacherId, ExamStatus.BORRADOR, new ArrayList<>());
        Mockito.when(userRepository.findByEmail("teacher@test.com")).thenReturn(Optional.of(teacher));
        Mockito.when(examRepository.findById(examId)).thenReturn(Optional.of(exam));

        // Act & Assert
        Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> examService.updateTopic("teacher@test.com", examId, UUID.randomUUID(), "New Name")
        );
    }

    @Test
    void removeTopic_validInput_removesTopic() {
        // Arrange
        UUID teacherId = UUID.randomUUID();
        UUID examId = UUID.randomUUID();
        UUID topicId = UUID.randomUUID();
        User teacher = createSampleTeacher(teacherId, "teacher@test.com");
        ExamTopic topic = createSampleTopic(topicId, "Topic", new ArrayList<>());
        Exam exam = createSampleExam(examId, teacherId, ExamStatus.BORRADOR, List.of(topic));
        Mockito.when(userRepository.findByEmail("teacher@test.com")).thenReturn(Optional.of(teacher));
        Mockito.when(examRepository.findById(examId)).thenReturn(Optional.of(exam));
        Mockito.when(examRepository.save(Mockito.any(Exam.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        Exam result = examService.removeTopic("teacher@test.com", examId, topicId);

        // Assert
        Assertions.assertNotNull(result);
        Assertions.assertEquals(0, result.getTopics().size());
    }

    @Test
    void removeTopic_topicNotFound_throwsIllegalArgumentException() {
        // Arrange
        UUID teacherId = UUID.randomUUID();
        UUID examId = UUID.randomUUID();
        User teacher = createSampleTeacher(teacherId, "teacher@test.com");
        Exam exam = createSampleExam(examId, teacherId, ExamStatus.BORRADOR, new ArrayList<>());
        Mockito.when(userRepository.findByEmail("teacher@test.com")).thenReturn(Optional.of(teacher));
        Mockito.when(examRepository.findById(examId)).thenReturn(Optional.of(exam));

        // Act & Assert
        Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> examService.removeTopic("teacher@test.com", examId, UUID.randomUUID())
        );
    }

    // ========================================== QUESTIONS ==========================================

    @Test
    void addQuestion_validInput_addsAndSaves() {
        // Arrange
        UUID teacherId = UUID.randomUUID();
        UUID examId = UUID.randomUUID();
        UUID topicId = UUID.randomUUID();
        User teacher = createSampleTeacher(teacherId, "teacher@test.com");
        ExamTopic topic = createSampleTopic(topicId, "Topic", new ArrayList<>());
        Exam exam = createSampleExam(examId, teacherId, ExamStatus.BORRADOR, List.of(topic));
        Mockito.when(userRepository.findByEmail("teacher@test.com")).thenReturn(Optional.of(teacher));
        Mockito.when(examRepository.findById(examId)).thenReturn(Optional.of(exam));
        Mockito.when(examRepository.save(Mockito.any(Exam.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        Exam result = examService.addQuestion("teacher@test.com", examId, topicId, "Q1", "A1", new BigDecimal("5.0"));

        // Assert
        Assertions.assertNotNull(result);
        Assertions.assertEquals(1, result.getTopics().get(0).getQuestions().size());
        Assertions.assertEquals("Q1", result.getTopics().get(0).getQuestions().get(0).getPrompt());
        Assertions.assertEquals("A1", result.getTopics().get(0).getQuestions().get(0).getModelAnswer());
        Assertions.assertEquals(0, new BigDecimal("5").compareTo(result.getTopics().get(0).getQuestions().get(0).getPoints()));
    }

    @Test
    void addQuestion_invalidPointsNull_throwsIllegalArgumentException() {
        // Arrange
        UUID teacherId = UUID.randomUUID();
        UUID examId = UUID.randomUUID();
        UUID topicId = UUID.randomUUID();
        User teacher = createSampleTeacher(teacherId, "teacher@test.com");
        ExamTopic topic = createSampleTopic(topicId, "Topic", new ArrayList<>());
        Exam exam = createSampleExam(examId, teacherId, ExamStatus.BORRADOR, List.of(topic));
        Mockito.when(userRepository.findByEmail("teacher@test.com")).thenReturn(Optional.of(teacher));
        Mockito.when(examRepository.findById(examId)).thenReturn(Optional.of(exam));

        // Act & Assert
        Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> examService.addQuestion("teacher@test.com", examId, topicId, "Q", "A", null)
        );
    }

    @Test
    void addQuestion_invalidPointsNegative_throwsIllegalArgumentException() {
        // Arrange
        UUID teacherId = UUID.randomUUID();
        UUID examId = UUID.randomUUID();
        UUID topicId = UUID.randomUUID();
        User teacher = createSampleTeacher(teacherId, "teacher@test.com");
        ExamTopic topic = createSampleTopic(topicId, "Topic", new ArrayList<>());
        Exam exam = createSampleExam(examId, teacherId, ExamStatus.BORRADOR, List.of(topic));
        Mockito.when(userRepository.findByEmail("teacher@test.com")).thenReturn(Optional.of(teacher));
        Mockito.when(examRepository.findById(examId)).thenReturn(Optional.of(exam));

        // Act & Assert
        Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> examService.addQuestion("teacher@test.com", examId, topicId, "Q", "A", new BigDecimal("-1"))
        );
    }

    @Test
    void addQuestion_invalidPointsExceedsTen_throwsIllegalArgumentException() {
        // Arrange
        UUID teacherId = UUID.randomUUID();
        UUID examId = UUID.randomUUID();
        UUID topicId = UUID.randomUUID();
        User teacher = createSampleTeacher(teacherId, "teacher@test.com");
        ExamTopic topic = createSampleTopic(topicId, "Topic", new ArrayList<>());
        Exam exam = createSampleExam(examId, teacherId, ExamStatus.BORRADOR, List.of(topic));
        Mockito.when(userRepository.findByEmail("teacher@test.com")).thenReturn(Optional.of(teacher));
        Mockito.when(examRepository.findById(examId)).thenReturn(Optional.of(exam));

        // Act & Assert
        Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> examService.addQuestion("teacher@test.com", examId, topicId, "Q", "A", new BigDecimal("10.5"))
        );
    }

    @Test
    void updateQuestion_validInput_updatesQuestionFields() {
        // Arrange
        UUID teacherId = UUID.randomUUID();
        UUID examId = UUID.randomUUID();
        UUID topicId = UUID.randomUUID();
        UUID questionId = UUID.randomUUID();
        User teacher = createSampleTeacher(teacherId, "teacher@test.com");
        ExamQuestion question = createSampleQuestion(questionId, "Q1", "A1", new BigDecimal("5"), 1);
        ExamTopic topic = createSampleTopic(topicId, "Topic", List.of(question));
        Exam exam = createSampleExam(examId, teacherId, ExamStatus.BORRADOR, List.of(topic));
        Mockito.when(userRepository.findByEmail("teacher@test.com")).thenReturn(Optional.of(teacher));
        Mockito.when(examRepository.findById(examId)).thenReturn(Optional.of(exam));
        Mockito.when(examRepository.save(Mockito.any(Exam.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        Exam result = examService.updateQuestion("teacher@test.com", examId, topicId, questionId, "New Q", "New A", new BigDecimal("6"));

        // Assert
        Assertions.assertNotNull(result);
        ExamQuestion resultQ = result.getTopics().get(0).getQuestions().get(0);
        Assertions.assertEquals("New Q", resultQ.getPrompt());
        Assertions.assertEquals("New A", resultQ.getModelAnswer());
        Assertions.assertEquals(0, new BigDecimal("6").compareTo(resultQ.getPoints()));
    }

    @Test
    void updateQuestion_questionNotFound_throwsIllegalArgumentException() {
        // Arrange
        UUID teacherId = UUID.randomUUID();
        UUID examId = UUID.randomUUID();
        UUID topicId = UUID.randomUUID();
        User teacher = createSampleTeacher(teacherId, "teacher@test.com");
        ExamTopic topic = createSampleTopic(topicId, "Topic", new ArrayList<>());
        Exam exam = createSampleExam(examId, teacherId, ExamStatus.BORRADOR, List.of(topic));
        Mockito.when(userRepository.findByEmail("teacher@test.com")).thenReturn(Optional.of(teacher));
        Mockito.when(examRepository.findById(examId)).thenReturn(Optional.of(exam));

        // Act & Assert
        Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> examService.updateQuestion("teacher@test.com", examId, topicId, UUID.randomUUID(), "Q", "A", new BigDecimal("5"))
        );
    }

    @Test
    void removeQuestion_validInput_removesAndReindexesQuestions() {
        // Arrange
        UUID teacherId = UUID.randomUUID();
        UUID examId = UUID.randomUUID();
        UUID topicId = UUID.randomUUID();
        UUID qId1 = UUID.randomUUID();
        UUID qId2 = UUID.randomUUID();
        User teacher = createSampleTeacher(teacherId, "teacher@test.com");
        ExamQuestion q1 = createSampleQuestion(qId1, "Q1", "A1", new BigDecimal("5"), 1);
        ExamQuestion q2 = createSampleQuestion(qId2, "Q2", "A2", new BigDecimal("5"), 2);
        // Note: safeQuestions sorts by displayOrder. We pass list containing both.
        ExamTopic topic = createSampleTopic(topicId, "Topic", List.of(q1, q2));
        Exam exam = createSampleExam(examId, teacherId, ExamStatus.BORRADOR, List.of(topic));
        Mockito.when(userRepository.findByEmail("teacher@test.com")).thenReturn(Optional.of(teacher));
        Mockito.when(examRepository.findById(examId)).thenReturn(Optional.of(exam));
        Mockito.when(examRepository.save(Mockito.any(Exam.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        Exam result = examService.removeQuestion("teacher@test.com", examId, topicId, qId1);

        // Assert
        Assertions.assertNotNull(result);
        List<ExamQuestion> questions = result.getTopics().get(0).getQuestions();
        Assertions.assertEquals(1, questions.size());
        Assertions.assertEquals(qId2, questions.get(0).getId());
        Assertions.assertEquals(1, questions.get(0).getDisplayOrder());
    }

    @Test
    void removeQuestion_questionNotFound_throwsIllegalArgumentException() {
        // Arrange
        UUID teacherId = UUID.randomUUID();
        UUID examId = UUID.randomUUID();
        UUID topicId = UUID.randomUUID();
        User teacher = createSampleTeacher(teacherId, "teacher@test.com");
        ExamTopic topic = createSampleTopic(topicId, "Topic", new ArrayList<>());
        Exam exam = createSampleExam(examId, teacherId, ExamStatus.BORRADOR, List.of(topic));
        Mockito.when(userRepository.findByEmail("teacher@test.com")).thenReturn(Optional.of(teacher));
        Mockito.when(examRepository.findById(examId)).thenReturn(Optional.of(exam));

        // Act & Assert
        Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> examService.removeQuestion("teacher@test.com", examId, topicId, UUID.randomUUID())
        );
    }

    // ========================================== PUBLISH ==========================================

    @Test
    void publish_withTenPointsTopicAndValidPrompts_changesStatusToPublicado() {
        // Arrange
        UUID teacherId = UUID.randomUUID();
        UUID examId = UUID.randomUUID();
        User teacher = createSampleTeacher(teacherId, "teacher@test.com");
        ExamQuestion question = createSampleQuestion(UUID.randomUUID(), "Prompt", "Answer", new BigDecimal("10.0"), 1);
        ExamTopic topic = createSampleTopic(UUID.randomUUID(), "Topic", List.of(question));
        Exam exam = createSampleExam(examId, teacherId, ExamStatus.BORRADOR, List.of(topic));
        Mockito.when(userRepository.findByEmail("teacher@test.com")).thenReturn(Optional.of(teacher));
        Mockito.when(examRepository.findById(examId)).thenReturn(Optional.of(exam));
        Mockito.when(examRepository.save(Mockito.any(Exam.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        Exam result = examService.publish("teacher@test.com", examId);

        // Assert
        Assertions.assertNotNull(result);
        Assertions.assertEquals(ExamStatus.PUBLICADO, result.getStatus());
        Assertions.assertNotNull(result.getPublishedAt());
    }

    @Test
    void publish_noTopics_throwsIllegalArgumentException() {
        // Arrange
        UUID teacherId = UUID.randomUUID();
        UUID examId = UUID.randomUUID();
        User teacher = createSampleTeacher(teacherId, "teacher@test.com");
        Exam exam = createSampleExam(examId, teacherId, ExamStatus.BORRADOR, new ArrayList<>());
        Mockito.when(userRepository.findByEmail("teacher@test.com")).thenReturn(Optional.of(teacher));
        Mockito.when(examRepository.findById(examId)).thenReturn(Optional.of(exam));

        // Act & Assert
        IllegalArgumentException exception = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> examService.publish("teacher@test.com", examId)
        );
        Assertions.assertEquals("El examen debe tener al menos un tema", exception.getMessage());
    }

    @Test
    void publish_topicWithNoQuestions_throwsIllegalArgumentException() {
        // Arrange
        UUID teacherId = UUID.randomUUID();
        UUID examId = UUID.randomUUID();
        User teacher = createSampleTeacher(teacherId, "teacher@test.com");
        ExamTopic topic = createSampleTopic(UUID.randomUUID(), "Topic", new ArrayList<>());
        Exam exam = createSampleExam(examId, teacherId, ExamStatus.BORRADOR, List.of(topic));
        Mockito.when(userRepository.findByEmail("teacher@test.com")).thenReturn(Optional.of(teacher));
        Mockito.when(examRepository.findById(examId)).thenReturn(Optional.of(exam));

        // Act & Assert
        IllegalArgumentException exception = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> examService.publish("teacher@test.com", examId)
        );
        Assertions.assertEquals("Cada tema debe tener al menos una pregunta", exception.getMessage());
    }

    @Test
    void publish_topicTotalNotTenPoints_throwsIllegalArgumentException() {
        // Arrange
        UUID teacherId = UUID.randomUUID();
        UUID examId = UUID.randomUUID();
        User teacher = createSampleTeacher(teacherId, "teacher@test.com");
        ExamQuestion question = createSampleQuestion(UUID.randomUUID(), "Prompt", "Answer", new BigDecimal("8.0"), 1);
        ExamTopic topic = createSampleTopic(UUID.randomUUID(), "Topic", List.of(question));
        Exam exam = createSampleExam(examId, teacherId, ExamStatus.BORRADOR, List.of(topic));
        Mockito.when(userRepository.findByEmail("teacher@test.com")).thenReturn(Optional.of(teacher));
        Mockito.when(examRepository.findById(examId)).thenReturn(Optional.of(exam));

        // Act & Assert
        IllegalArgumentException exception = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> examService.publish("teacher@test.com", examId)
        );
        Assertions.assertEquals("Cada tema debe sumar exactamente 10 puntos", exception.getMessage());
    }

    @Test
    void publish_blankQuestionPrompt_throwsIllegalArgumentException() {
        // Arrange
        UUID teacherId = UUID.randomUUID();
        UUID examId = UUID.randomUUID();
        User teacher = createSampleTeacher(teacherId, "teacher@test.com");
        ExamQuestion question = createSampleQuestion(UUID.randomUUID(), "   ", "Answer", new BigDecimal("10.0"), 1);
        ExamTopic topic = createSampleTopic(UUID.randomUUID(), "Topic A", List.of(question));
        Exam exam = createSampleExam(examId, teacherId, ExamStatus.BORRADOR, List.of(topic));
        Mockito.when(userRepository.findByEmail("teacher@test.com")).thenReturn(Optional.of(teacher));
        Mockito.when(examRepository.findById(examId)).thenReturn(Optional.of(exam));

        // Act & Assert
        IllegalArgumentException exception = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> examService.publish("teacher@test.com", examId)
        );
        Assertions.assertTrue(exception.getMessage().contains("Falta enunciado en Topic A · Pregunta 1"));
    }

    @Test
    void publish_blankQuestionModelAnswer_throwsIllegalArgumentException() {
        // Arrange
        UUID teacherId = UUID.randomUUID();
        UUID examId = UUID.randomUUID();
        User teacher = createSampleTeacher(teacherId, "teacher@test.com");
        ExamQuestion question = createSampleQuestion(UUID.randomUUID(), "Prompt Text", "", new BigDecimal("10.0"), 1);
        ExamTopic topic = createSampleTopic(UUID.randomUUID(), "Topic A", List.of(question));
        Exam exam = createSampleExam(examId, teacherId, ExamStatus.BORRADOR, List.of(topic));
        Mockito.when(userRepository.findByEmail("teacher@test.com")).thenReturn(Optional.of(teacher));
        Mockito.when(examRepository.findById(examId)).thenReturn(Optional.of(exam));

        // Act & Assert
        IllegalArgumentException exception = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> examService.publish("teacher@test.com", examId)
        );
        Assertions.assertTrue(exception.getMessage().contains("Falta respuesta modelo en Topic A · Pregunta 1"));
    }

    // ========================================== JSON VALIDATIONS (TREES & TABLES) ==========================================

    @Test
    void publish_emptyDecisionTreeModelAnswer_throwsIllegalArgumentException() {
        // Arrange
        UUID teacherId = UUID.randomUUID();
        UUID examId = UUID.randomUUID();
        User teacher = createSampleTeacher(teacherId, "teacher@test.com");
        // EMPTY_DECISION_TREE
        ExamQuestion question = createSampleQuestion(UUID.randomUUID(), "Prompt", "7TEST_DECISION_TREE:{\"nodes\":[],\"edges\":[]}", new BigDecimal("10.0"), 1);
        ExamTopic topic = createSampleTopic(UUID.randomUUID(), "Topic", List.of(question));
        Exam exam = createSampleExam(examId, teacherId, ExamStatus.BORRADOR, List.of(topic));
        Mockito.when(userRepository.findByEmail("teacher@test.com")).thenReturn(Optional.of(teacher));
        Mockito.when(examRepository.findById(examId)).thenReturn(Optional.of(exam));

        // Act & Assert
        Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> examService.publish("teacher@test.com", examId)
        );
    }

    @Test
    void publish_decisionTreeWithOnlyBlankNodeTextAndBlankEdgeLabel_throwsIllegalArgumentException() {
        // Arrange
        UUID teacherId = UUID.randomUUID();
        UUID examId = UUID.randomUUID();
        User teacher = createSampleTeacher(teacherId, "teacher@test.com");
        String emptyTreeJson = "7TEST_DECISION_TREE:{\"nodes\":[{\"text\":\"\"}],\"edges\":[{\"label\":\"   \"}]}";
        ExamQuestion question = createSampleQuestion(UUID.randomUUID(), "Prompt", emptyTreeJson, new BigDecimal("10.0"), 1);
        ExamTopic topic = createSampleTopic(UUID.randomUUID(), "Topic", List.of(question));
        Exam exam = createSampleExam(examId, teacherId, ExamStatus.BORRADOR, List.of(topic));
        Mockito.when(userRepository.findByEmail("teacher@test.com")).thenReturn(Optional.of(teacher));
        Mockito.when(examRepository.findById(examId)).thenReturn(Optional.of(exam));

        // Act & Assert
        Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> examService.publish("teacher@test.com", examId)
        );
    }

    @Test
    void publish_decisionTreeWithNonBlankNodeText_publishesExam() {
        // Arrange
        UUID teacherId = UUID.randomUUID();
        UUID examId = UUID.randomUUID();
        User teacher = createSampleTeacher(teacherId, "teacher@test.com");
        String treeWithNodeText = "7TEST_DECISION_TREE:{\"nodes\":[{\"text\":\"Condition 1\"}],\"edges\":[]}";
        ExamQuestion question = createSampleQuestion(UUID.randomUUID(), "Prompt", treeWithNodeText, new BigDecimal("10.0"), 1);
        ExamTopic topic = createSampleTopic(UUID.randomUUID(), "Topic", List.of(question));
        Exam exam = createSampleExam(examId, teacherId, ExamStatus.BORRADOR, List.of(topic));
        Mockito.when(userRepository.findByEmail("teacher@test.com")).thenReturn(Optional.of(teacher));
        Mockito.when(examRepository.findById(examId)).thenReturn(Optional.of(exam));
        Mockito.when(examRepository.save(Mockito.any(Exam.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        Exam result = examService.publish("teacher@test.com", examId);

        // Assert
        Assertions.assertNotNull(result);
        Assertions.assertEquals(ExamStatus.PUBLICADO, result.getStatus());
    }

    @Test
    void publish_decisionTreeWithNonBlankEdgeLabel_publishesExam() {
        // Arrange
        UUID teacherId = UUID.randomUUID();
        UUID examId = UUID.randomUUID();
        User teacher = createSampleTeacher(teacherId, "teacher@test.com");
        String treeWithEdgeLabel = "7TEST_DECISION_TREE:{\"nodes\":[],\"edges\":[{\"label\":\"Yes\"}]}";
        ExamQuestion question = createSampleQuestion(UUID.randomUUID(), "Prompt", treeWithEdgeLabel, new BigDecimal("10.0"), 1);
        ExamTopic topic = createSampleTopic(UUID.randomUUID(), "Topic", List.of(question));
        Exam exam = createSampleExam(examId, teacherId, ExamStatus.BORRADOR, List.of(topic));
        Mockito.when(userRepository.findByEmail("teacher@test.com")).thenReturn(Optional.of(teacher));
        Mockito.when(examRepository.findById(examId)).thenReturn(Optional.of(exam));
        Mockito.when(examRepository.save(Mockito.any(Exam.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        Exam result = examService.publish("teacher@test.com", examId);

        // Assert
        Assertions.assertNotNull(result);
        Assertions.assertEquals(ExamStatus.PUBLICADO, result.getStatus());
    }

    @Test
    void publish_decisionTreeInvalidJson_throwsIllegalArgumentException() {
        // Arrange
        UUID teacherId = UUID.randomUUID();
        UUID examId = UUID.randomUUID();
        User teacher = createSampleTeacher(teacherId, "teacher@test.com");
        String badTreeJson = "7TEST_DECISION_TREE:invalid-json-structure-here";
        ExamQuestion question = createSampleQuestion(UUID.randomUUID(), "Prompt", badTreeJson, new BigDecimal("10.0"), 1);
        ExamTopic topic = createSampleTopic(UUID.randomUUID(), "Topic", List.of(question));
        Exam exam = createSampleExam(examId, teacherId, ExamStatus.BORRADOR, List.of(topic));
        Mockito.when(userRepository.findByEmail("teacher@test.com")).thenReturn(Optional.of(teacher));
        Mockito.when(examRepository.findById(examId)).thenReturn(Optional.of(exam));

        // Act & Assert
        Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> examService.publish("teacher@test.com", examId)
        );
    }

    @Test
    void publish_emptyDecisionTableModelAnswer_throwsIllegalArgumentException() {
        // Arrange
        UUID teacherId = UUID.randomUUID();
        UUID examId = UUID.randomUUID();
        User teacher = createSampleTeacher(teacherId, "teacher@test.com");
        // EMPTY_DECISION_TABLE
        ExamQuestion question = createSampleQuestion(UUID.randomUUID(), "Prompt", "7TEST_DECISION_TABLE:{\"rows\":2,\"cols\":2,\"cells\":[[\"\",\"\"],[\"\",\"\"]]}", new BigDecimal("10.0"), 1);
        ExamTopic topic = createSampleTopic(UUID.randomUUID(), "Topic", List.of(question));
        Exam exam = createSampleExam(examId, teacherId, ExamStatus.BORRADOR, List.of(topic));
        Mockito.when(userRepository.findByEmail("teacher@test.com")).thenReturn(Optional.of(teacher));
        Mockito.when(examRepository.findById(examId)).thenReturn(Optional.of(exam));

        // Act & Assert
        Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> examService.publish("teacher@test.com", examId)
        );
    }

    @Test
    void publish_decisionTableWithOnlyBlankCellsInRowOneAndAbove_throwsIllegalArgumentException() {
        // Arrange
        UUID teacherId = UUID.randomUUID();
        UUID examId = UUID.randomUUID();
        User teacher = createSampleTeacher(teacherId, "teacher@test.com");
        // The first row (headers) and all other rows are blank
        String blankTableJson = "7TEST_DECISION_TABLE:{\"cells\":[[\"\",\"\"],[\"\",\"\"],[\"\",\"\"]]}";
        ExamQuestion question = createSampleQuestion(UUID.randomUUID(), "Prompt", blankTableJson, new BigDecimal("10.0"), 1);
        ExamTopic topic = createSampleTopic(UUID.randomUUID(), "Topic", List.of(question));
        Exam exam = createSampleExam(examId, teacherId, ExamStatus.BORRADOR, List.of(topic));
        Mockito.when(userRepository.findByEmail("teacher@test.com")).thenReturn(Optional.of(teacher));
        Mockito.when(examRepository.findById(examId)).thenReturn(Optional.of(exam));

        // Act & Assert
        Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> examService.publish("teacher@test.com", examId)
        );
    }

    @Test
    void publish_decisionTableWithNonBlankCellInRowOneOrAbove_publishesExam() {
        // Arrange
        UUID teacherId = UUID.randomUUID();
        UUID examId = UUID.randomUUID();
        User teacher = createSampleTeacher(teacherId, "teacher@test.com");
        String tableWithCellText = "7TEST_DECISION_TABLE:{\"cells\":[[\"Header1\",\"Header2\"],[\"CellValue\",\"\"],[\"\",\"\"]]}";
        ExamQuestion question = createSampleQuestion(UUID.randomUUID(), "Prompt", tableWithCellText, new BigDecimal("10.0"), 1);
        ExamTopic topic = createSampleTopic(UUID.randomUUID(), "Topic", List.of(question));
        Exam exam = createSampleExam(examId, teacherId, ExamStatus.BORRADOR, List.of(topic));
        Mockito.when(userRepository.findByEmail("teacher@test.com")).thenReturn(Optional.of(teacher));
        Mockito.when(examRepository.findById(examId)).thenReturn(Optional.of(exam));
        Mockito.when(examRepository.save(Mockito.any(Exam.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        Exam result = examService.publish("teacher@test.com", examId);

        // Assert
        Assertions.assertNotNull(result);
        Assertions.assertEquals(ExamStatus.PUBLICADO, result.getStatus());
    }

    @Test
    void publish_decisionTableInvalidJson_throwsIllegalArgumentException() {
        // Arrange
        UUID teacherId = UUID.randomUUID();
        UUID examId = UUID.randomUUID();
        User teacher = createSampleTeacher(teacherId, "teacher@test.com");
        String badTableJson = "7TEST_DECISION_TABLE:invalid-json-structure";
        ExamQuestion question = createSampleQuestion(UUID.randomUUID(), "Prompt", badTableJson, new BigDecimal("10.0"), 1);
        ExamTopic topic = createSampleTopic(UUID.randomUUID(), "Topic", List.of(question));
        Exam exam = createSampleExam(examId, teacherId, ExamStatus.BORRADOR, List.of(topic));
        Mockito.when(userRepository.findByEmail("teacher@test.com")).thenReturn(Optional.of(teacher));
        Mockito.when(examRepository.findById(examId)).thenReturn(Optional.of(exam));

        // Act & Assert
        Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> examService.publish("teacher@test.com", examId)
        );
    }

    // ========================================== CLOSE ==========================================

    @Test
    void close_statusIsAlreadyCerrado_returnsDirectlyWithoutSaving() {
        // Arrange
        UUID teacherId = UUID.randomUUID();
        UUID examId = UUID.randomUUID();
        User teacher = createSampleTeacher(teacherId, "teacher@test.com");
        Exam exam = createSampleExam(examId, teacherId, ExamStatus.CERRADO, new ArrayList<>());
        Mockito.when(userRepository.findByEmail("teacher@test.com")).thenReturn(Optional.of(teacher));
        Mockito.when(examRepository.findById(examId)).thenReturn(Optional.of(exam));

        // Act
        Exam result = examService.close("teacher@test.com", examId);

        // Assert
        Assertions.assertSame(exam, result);
        Mockito.verify(examRepository, Mockito.never()).save(Mockito.any(Exam.class));
    }

    @Test
    void close_statusIsPublicado_savesAsCerrado() {
        // Arrange
        UUID teacherId = UUID.randomUUID();
        UUID examId = UUID.randomUUID();
        User teacher = createSampleTeacher(teacherId, "teacher@test.com");
        Exam exam = createSampleExam(examId, teacherId, ExamStatus.PUBLICADO, new ArrayList<>());
        Mockito.when(userRepository.findByEmail("teacher@test.com")).thenReturn(Optional.of(teacher));
        Mockito.when(examRepository.findById(examId)).thenReturn(Optional.of(exam));
        Mockito.when(examRepository.save(Mockito.any(Exam.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        Exam result = examService.close("teacher@test.com", examId);

        // Assert
        Assertions.assertNotNull(result);
        Assertions.assertEquals(ExamStatus.CERRADO, result.getStatus());
        Mockito.verify(examRepository, Mockito.times(1)).save(Mockito.any(Exam.class));
    }

    // ========================================== LIST FOR TEACHER / SUPERVISION / STUDENTS ==========================================

    @Test
    void listForTeacher_validTeacher_returnsExamsList() {
        // Arrange
        UUID teacherId = UUID.randomUUID();
        User teacher = createSampleTeacher(teacherId, "teacher@test.com");
        List<Exam> examsList = List.of(createSampleExam(UUID.randomUUID(), teacherId, ExamStatus.BORRADOR, new ArrayList<>()));
        Mockito.when(userRepository.findByEmail("teacher@test.com")).thenReturn(Optional.of(teacher));
        Mockito.when(examRepository.findByTeacherId(teacherId)).thenReturn(examsList);

        // Act
        List<Exam> result = examService.listForTeacher("teacher@test.com");

        // Assert
        Assertions.assertEquals(examsList, result);
    }

    @Test
    void listForSupervision_nullStatus_returnsAllExams() {
        // Arrange
        List<Exam> allExams = List.of(createSampleExam(UUID.randomUUID(), UUID.randomUUID(), ExamStatus.BORRADOR, new ArrayList<>()));
        Mockito.when(examRepository.findAll()).thenReturn(allExams);

        // Act
        List<Exam> result = examService.listForSupervision(null);

        // Assert
        Assertions.assertEquals(allExams, result);
    }

    @Test
    void listForSupervision_withStatus_returnsFilteredExams() {
        // Arrange
        List<Exam> publishedExams = List.of(createSampleExam(UUID.randomUUID(), UUID.randomUUID(), ExamStatus.PUBLICADO, new ArrayList<>()));
        Mockito.when(examRepository.findByStatus(ExamStatus.PUBLICADO)).thenReturn(publishedExams);

        // Act
        List<Exam> result = examService.listForSupervision(ExamStatus.PUBLICADO);

        // Assert
        Assertions.assertEquals(publishedExams, result);
    }

    @Test
    void listPublishedForStudents_always_returnsPublishedAndClosedExams() {
        // Arrange
        Exam pub = createSampleExam(UUID.randomUUID(), UUID.randomUUID(), ExamStatus.PUBLICADO, new ArrayList<>());
        Exam closed = createSampleExam(UUID.randomUUID(), UUID.randomUUID(), ExamStatus.CERRADO, new ArrayList<>());
        Mockito.when(examRepository.findByStatus(ExamStatus.PUBLICADO)).thenReturn(List.of(pub));
        Mockito.when(examRepository.findByStatus(ExamStatus.CERRADO)).thenReturn(List.of(closed));

        // Act
        List<Exam> result = examService.listPublishedForStudents();

        // Assert
        Assertions.assertEquals(2, result.size());
        Assertions.assertTrue(result.contains(pub));
        Assertions.assertTrue(result.contains(closed));
    }

    // ========================================== FIND BY ID ==========================================

    @Test
    void findById_examDoesNotExist_throwsExamNotFoundException() {
        // Arrange
        UUID examId = UUID.randomUUID();
        Mockito.when(examRepository.findById(examId)).thenReturn(Optional.empty());

        // Act & Assert
        Assertions.assertThrows(
                ExamNotFoundException.class,
                () -> examService.findById(examId)
        );
    }

    // ========================================== EXTRA TIME ==========================================

    @Test
    void addExtraTime_validInput_updatesDurationAndSetsFlag() {
        // Arrange
        UUID teacherId = UUID.randomUUID();
        UUID examId = UUID.randomUUID();
        User teacher = createSampleTeacher(teacherId, "teacher@test.com");
        Exam exam = createSampleExam(examId, teacherId, ExamStatus.PUBLICADO, new ArrayList<>());
        Mockito.when(userRepository.findByEmail("teacher@test.com")).thenReturn(Optional.of(teacher));
        Mockito.when(examRepository.findById(examId)).thenReturn(Optional.of(exam));
        Mockito.when(examRepository.save(Mockito.any(Exam.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        Exam result = examService.addExtraTime("teacher@test.com", examId, 30);

        // Assert
        Assertions.assertNotNull(result);
        Assertions.assertEquals(90, result.getDurationMinutes());
        Assertions.assertTrue(result.isExtraTimeUsed());
    }

    @Test
    void addExtraTime_notPublishedStatus_throwsIllegalArgumentException() {
        // Arrange
        UUID teacherId = UUID.randomUUID();
        UUID examId = UUID.randomUUID();
        User teacher = createSampleTeacher(teacherId, "teacher@test.com");
        Exam exam = createSampleExam(examId, teacherId, ExamStatus.BORRADOR, new ArrayList<>());
        Mockito.when(userRepository.findByEmail("teacher@test.com")).thenReturn(Optional.of(teacher));
        Mockito.when(examRepository.findById(examId)).thenReturn(Optional.of(exam));

        // Act & Assert
        Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> examService.addExtraTime("teacher@test.com", examId, 10)
        );
    }

    @Test
    void addExtraTime_alreadyUsed_throwsIllegalArgumentException() {
        // Arrange
        UUID teacherId = UUID.randomUUID();
        UUID examId = UUID.randomUUID();
        User teacher = createSampleTeacher(teacherId, "teacher@test.com");
        Exam exam = createSampleExam(examId, teacherId, ExamStatus.PUBLICADO, new ArrayList<>()).toBuilder()
                .extraTimeUsed(true)
                .build();
        Mockito.when(userRepository.findByEmail("teacher@test.com")).thenReturn(Optional.of(teacher));
        Mockito.when(examRepository.findById(examId)).thenReturn(Optional.of(exam));

        // Act & Assert
        Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> examService.addExtraTime("teacher@test.com", examId, 10)
        );
    }

    @Test
    void addExtraTime_invalidMinutesZero_throwsIllegalArgumentException() {
        // Arrange
        UUID teacherId = UUID.randomUUID();
        UUID examId = UUID.randomUUID();
        User teacher = createSampleTeacher(teacherId, "teacher@test.com");
        Exam exam = createSampleExam(examId, teacherId, ExamStatus.PUBLICADO, new ArrayList<>());
        Mockito.when(userRepository.findByEmail("teacher@test.com")).thenReturn(Optional.of(teacher));
        Mockito.when(examRepository.findById(examId)).thenReturn(Optional.of(exam));

        // Act & Assert
        Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> examService.addExtraTime("teacher@test.com", examId, 0)
        );
    }

    @Test
    void addExtraTime_invalidMinutesTooLarge_throwsIllegalArgumentException() {
        // Arrange
        UUID teacherId = UUID.randomUUID();
        UUID examId = UUID.randomUUID();
        User teacher = createSampleTeacher(teacherId, "teacher@test.com");
        Exam exam = createSampleExam(examId, teacherId, ExamStatus.PUBLICADO, new ArrayList<>());
        Mockito.when(userRepository.findByEmail("teacher@test.com")).thenReturn(Optional.of(teacher));
        Mockito.when(examRepository.findById(examId)).thenReturn(Optional.of(exam));

        // Act & Assert
        Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> examService.addExtraTime("teacher@test.com", examId, 61)
        );
    }

    // ========================================== FEEDBACK ==========================================

    @Test
    void publishFeedback_statusBorrador_throwsIllegalArgumentException() {
        // Arrange
        UUID teacherId = UUID.randomUUID();
        UUID examId = UUID.randomUUID();
        User teacher = createSampleTeacher(teacherId, "teacher@test.com");
        Exam exam = createSampleExam(examId, teacherId, ExamStatus.BORRADOR, new ArrayList<>());
        Mockito.when(userRepository.findByEmail("teacher@test.com")).thenReturn(Optional.of(teacher));
        Mockito.when(examRepository.findById(examId)).thenReturn(Optional.of(exam));

        // Act & Assert
        Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> examService.publishFeedback("teacher@test.com", examId)
        );
    }

    @Test
    void publishFeedback_statusClosed_savesWithFeedbackPublished() {
        // Arrange
        UUID teacherId = UUID.randomUUID();
        UUID examId = UUID.randomUUID();
        User teacher = createSampleTeacher(teacherId, "teacher@test.com");
        Exam exam = createSampleExam(examId, teacherId, ExamStatus.CERRADO, new ArrayList<>());
        Mockito.when(userRepository.findByEmail("teacher@test.com")).thenReturn(Optional.of(teacher));
        Mockito.when(examRepository.findById(examId)).thenReturn(Optional.of(exam));
        Mockito.when(examRepository.save(Mockito.any(Exam.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        Exam result = examService.publishFeedback("teacher@test.com", examId);

        // Assert
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.isFeedbackPublished());
    }

    // ========================================== DELETE ==========================================

    @Test
    void deleteExam_statusBorrador_deletesSuccessfully() {
        // Arrange
        UUID teacherId = UUID.randomUUID();
        UUID examId = UUID.randomUUID();
        User teacher = createSampleTeacher(teacherId, "teacher@test.com");
        Exam exam = createSampleExam(examId, teacherId, ExamStatus.BORRADOR, new ArrayList<>());
        Mockito.when(userRepository.findByEmail("teacher@test.com")).thenReturn(Optional.of(teacher));
        Mockito.when(examRepository.findById(examId)).thenReturn(Optional.of(exam));

        // Act
        examService.deleteExam("teacher@test.com", examId);

        // Assert
        Mockito.verify(examRepository, Mockito.times(1)).deleteById(examId);
    }

    @Test
    void deleteExam_statusPublicado_throwsIllegalArgumentException() {
        // Arrange
        UUID teacherId = UUID.randomUUID();
        UUID examId = UUID.randomUUID();
        User teacher = createSampleTeacher(teacherId, "teacher@test.com");
        Exam exam = createSampleExam(examId, teacherId, ExamStatus.PUBLICADO, new ArrayList<>());
        Mockito.when(userRepository.findByEmail("teacher@test.com")).thenReturn(Optional.of(teacher));
        Mockito.when(examRepository.findById(examId)).thenReturn(Optional.of(exam));

        // Act & Assert
        Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> examService.deleteExam("teacher@test.com", examId)
        );
        Mockito.verify(examRepository, Mockito.never()).deleteById(Mockito.any(UUID.class));
    }

    @Test
    void deleteExam_statusClosedWithSubmissions_throwsIllegalArgumentException() {
        // Arrange
        UUID teacherId = UUID.randomUUID();
        UUID examId = UUID.randomUUID();
        User teacher = createSampleTeacher(teacherId, "teacher@test.com");
        Exam exam = createSampleExam(examId, teacherId, ExamStatus.CERRADO, new ArrayList<>());
        Mockito.when(userRepository.findByEmail("teacher@test.com")).thenReturn(Optional.of(teacher));
        Mockito.when(examRepository.findById(examId)).thenReturn(Optional.of(exam));
        Mockito.when(submissionRepository.findByExamId(examId)).thenReturn(List.of(Mockito.mock(com.seventest.domain.model.ExamSubmission.class)));

        // Act & Assert
        Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> examService.deleteExam("teacher@test.com", examId)
        );
        Mockito.verify(examRepository, Mockito.never()).deleteById(Mockito.any(UUID.class));
    }

    @Test
    void deleteExam_statusClosedWithoutSubmissions_deletesSuccessfully() {
        // Arrange
        UUID teacherId = UUID.randomUUID();
        UUID examId = UUID.randomUUID();
        User teacher = createSampleTeacher(teacherId, "teacher@test.com");
        Exam exam = createSampleExam(examId, teacherId, ExamStatus.CERRADO, new ArrayList<>());
        Mockito.when(userRepository.findByEmail("teacher@test.com")).thenReturn(Optional.of(teacher));
        Mockito.when(examRepository.findById(examId)).thenReturn(Optional.of(exam));
        Mockito.when(submissionRepository.findByExamId(examId)).thenReturn(Collections.emptyList());

        // Act
        examService.deleteExam("teacher@test.com", examId);

        // Assert
        Mockito.verify(examRepository, Mockito.times(1)).deleteById(examId);
    }

    @Test
    void addExtraTime_durationMinutesIsNull_usesZeroAsBase() {
        // Arrange
        UUID teacherId = UUID.randomUUID();
        UUID examId = UUID.randomUUID();
        User teacher = createSampleTeacher(teacherId, "teacher@test.com");
        Exam exam = createSampleExam(examId, teacherId, ExamStatus.PUBLICADO, new ArrayList<>()).toBuilder()
                .durationMinutes(null)
                .build();
        Mockito.when(userRepository.findByEmail("teacher@test.com")).thenReturn(Optional.of(teacher));
        Mockito.when(examRepository.findById(examId)).thenReturn(Optional.of(exam));
        Mockito.when(examRepository.save(Mockito.any(Exam.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        Exam result = examService.addExtraTime("teacher@test.com", examId, 30);

        // Assert
        Assertions.assertNotNull(result);
        Assertions.assertEquals(30, result.getDurationMinutes());
    }

    @Test
    void publish_withLongPrompt_trimsHintToSixtyCharacters() {
        // Arrange
        UUID teacherId = UUID.randomUUID();
        UUID examId = UUID.randomUUID();
        User teacher = createSampleTeacher(teacherId, "teacher@test.com");
        String longPrompt = "A".repeat(70);
        // This will trigger the missing model answer validation to print the hint
        ExamQuestion question = createSampleQuestion(UUID.randomUUID(), longPrompt, "", new BigDecimal("10.0"), 1);
        ExamTopic topic = createSampleTopic(UUID.randomUUID(), "Topic", List.of(question));
        Exam exam = createSampleExam(examId, teacherId, ExamStatus.BORRADOR, List.of(topic));
        Mockito.when(userRepository.findByEmail("teacher@test.com")).thenReturn(Optional.of(teacher));
        Mockito.when(examRepository.findById(examId)).thenReturn(Optional.of(exam));

        // Act & Assert
        IllegalArgumentException exception = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> examService.publish("teacher@test.com", examId)
        );
        String expectedHint = "A".repeat(60);
        Assertions.assertTrue(exception.getMessage().contains("Falta respuesta modelo en Topic · Pregunta 1: " + expectedHint));
    }

    @Test
    void publish_withNullPrompt_throwsIllegalArgumentException() {
        // Arrange
        UUID teacherId = UUID.randomUUID();
        UUID examId = UUID.randomUUID();
        User teacher = createSampleTeacher(teacherId, "teacher@test.com");
        ExamQuestion question = createSampleQuestion(UUID.randomUUID(), null, "Answer", new BigDecimal("10.0"), 1);
        ExamTopic topic = createSampleTopic(UUID.randomUUID(), "Topic", List.of(question));
        Exam exam = createSampleExam(examId, teacherId, ExamStatus.BORRADOR, List.of(topic));
        Mockito.when(userRepository.findByEmail("teacher@test.com")).thenReturn(Optional.of(teacher));
        Mockito.when(examRepository.findById(examId)).thenReturn(Optional.of(exam));

        // Act & Assert
        Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> examService.publish("teacher@test.com", examId)
        );
    }

    @Test
    void create_withNullTitle_throwsIllegalArgumentException() {
        // Arrange
        User teacher = createSampleTeacher(UUID.randomUUID(), "teacher@test.com");
        Mockito.when(userRepository.findByEmail("teacher@test.com")).thenReturn(Optional.of(teacher));

        // Act & Assert
        Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> examService.create("teacher@test.com", null, "D", "C", null, 60)
        );
    }

    @Test
    void publish_withNullTopicsList_throwsIllegalArgumentException() {
        // Arrange
        UUID teacherId = UUID.randomUUID();
        UUID examId = UUID.randomUUID();
        User teacher = createSampleTeacher(teacherId, "teacher@test.com");
        Exam exam = createSampleExam(examId, teacherId, ExamStatus.BORRADOR, null);
        Mockito.when(userRepository.findByEmail("teacher@test.com")).thenReturn(Optional.of(teacher));
        Mockito.when(examRepository.findById(examId)).thenReturn(Optional.of(exam));

        // Act & Assert
        Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> examService.publish("teacher@test.com", examId)
        );
    }

    @Test
    void publish_withNullQuestionsList_throwsIllegalArgumentException() {
        // Arrange
        UUID teacherId = UUID.randomUUID();
        UUID examId = UUID.randomUUID();
        User teacher = createSampleTeacher(teacherId, "teacher@test.com");
        ExamTopic topic = createSampleTopic(UUID.randomUUID(), "Topic", null);
        Exam exam = createSampleExam(examId, teacherId, ExamStatus.BORRADOR, List.of(topic));
        Mockito.when(userRepository.findByEmail("teacher@test.com")).thenReturn(Optional.of(teacher));
        Mockito.when(examRepository.findById(examId)).thenReturn(Optional.of(exam));

        // Act & Assert
        Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> examService.publish("teacher@test.com", examId)
        );
    }

    @Test
    void updateQuestion_withMultipleQuestions_coversTernaryFalseBranch() {
        // Arrange
        UUID teacherId = UUID.randomUUID();
        UUID examId = UUID.randomUUID();
        UUID topicId = UUID.randomUUID();
        UUID questionIdToUpdate = UUID.randomUUID();
        UUID questionIdToKeep = UUID.randomUUID();
        User teacher = createSampleTeacher(teacherId, "teacher@test.com");
        ExamQuestion qToUpdate = createSampleQuestion(questionIdToUpdate, "Q1", "A1", new BigDecimal("5"), 1);
        ExamQuestion qToKeep = createSampleQuestion(questionIdToKeep, "Q2", "A2", new BigDecimal("5"), 2);
        ExamTopic topic = createSampleTopic(topicId, "Topic", List.of(qToUpdate, qToKeep));
        Exam exam = createSampleExam(examId, teacherId, ExamStatus.BORRADOR, List.of(topic));

        Mockito.when(userRepository.findByEmail("teacher@test.com")).thenReturn(Optional.of(teacher));
        Mockito.when(examRepository.findById(examId)).thenReturn(Optional.of(exam));
        Mockito.when(examRepository.save(Mockito.any(Exam.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        Exam result = examService.updateQuestion("teacher@test.com", examId, topicId, questionIdToUpdate, "New Q1", "New A1", new BigDecimal("4"));

        // Assert
        Assertions.assertNotNull(result);
        List<ExamQuestion> questions = result.getTopics().get(0).getQuestions();
        Assertions.assertEquals(2, questions.size());
        ExamQuestion updated = questions.stream().filter(q -> q.getId().equals(questionIdToUpdate)).findFirst().get();
        ExamQuestion kept = questions.stream().filter(q -> q.getId().equals(questionIdToKeep)).findFirst().get();
        Assertions.assertEquals("New Q1", updated.getPrompt());
        Assertions.assertEquals("Q2", kept.getPrompt());
    }

    @Test
    void updateQuestion_withMultipleTopics_coversTernaryFalseBranch() {
        // Arrange
        UUID teacherId = UUID.randomUUID();
        UUID examId = UUID.randomUUID();
        UUID topicIdToUpdate = UUID.randomUUID();
        UUID topicIdToKeep = UUID.randomUUID();
        UUID questionId = UUID.randomUUID();
        User teacher = createSampleTeacher(teacherId, "teacher@test.com");
        ExamQuestion question = createSampleQuestion(questionId, "Q1", "A1", new BigDecimal("5"), 1);
        ExamTopic topicToUpdate = createSampleTopic(topicIdToUpdate, "Topic 1", List.of(question));
        ExamTopic topicToKeep = createSampleTopic(topicIdToKeep, "Topic 2", new ArrayList<>());
        Exam exam = createSampleExam(examId, teacherId, ExamStatus.BORRADOR, List.of(topicToUpdate, topicToKeep));

        Mockito.when(userRepository.findByEmail("teacher@test.com")).thenReturn(Optional.of(teacher));
        Mockito.when(examRepository.findById(examId)).thenReturn(Optional.of(exam));
        Mockito.when(examRepository.save(Mockito.any(Exam.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        Exam result = examService.updateQuestion("teacher@test.com", examId, topicIdToUpdate, questionId, "New Q", "New A", new BigDecimal("5"));

        // Assert
        Assertions.assertNotNull(result);
        Assertions.assertEquals(2, result.getTopics().size());
    }

    @Test
    void removeQuestion_withMultipleTopics_coversTernaryFalseBranch() {
        // Arrange
        UUID teacherId = UUID.randomUUID();
        UUID examId = UUID.randomUUID();
        UUID topicIdToRemove = UUID.randomUUID();
        UUID topicIdToKeep = UUID.randomUUID();
        UUID questionId = UUID.randomUUID();
        User teacher = createSampleTeacher(teacherId, "teacher@test.com");
        ExamQuestion question = createSampleQuestion(questionId, "Q1", "A1", new BigDecimal("5"), 1);
        ExamTopic topicToRemove = createSampleTopic(topicIdToRemove, "Topic 1", List.of(question));
        ExamTopic topicToKeep = createSampleTopic(topicIdToKeep, "Topic 2", new ArrayList<>());
        Exam exam = createSampleExam(examId, teacherId, ExamStatus.BORRADOR, List.of(topicToRemove, topicToKeep));

        Mockito.when(userRepository.findByEmail("teacher@test.com")).thenReturn(Optional.of(teacher));
        Mockito.when(examRepository.findById(examId)).thenReturn(Optional.of(exam));
        Mockito.when(examRepository.save(Mockito.any(Exam.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        Exam result = examService.removeQuestion("teacher@test.com", examId, topicIdToRemove, questionId);

        // Assert
        Assertions.assertNotNull(result);
        Assertions.assertEquals(2, result.getTopics().size());
    }

    @Test
    void addQuestion_withMultipleTopics_coversTernaryFalseBranch() {
        // Arrange
        UUID teacherId = UUID.randomUUID();
        UUID examId = UUID.randomUUID();
        UUID topicIdToAdd = UUID.randomUUID();
        UUID topicIdToKeep = UUID.randomUUID();
        User teacher = createSampleTeacher(teacherId, "teacher@test.com");
        ExamTopic topicToAdd = createSampleTopic(topicIdToAdd, "Topic 1", new ArrayList<>());
        ExamTopic topicToKeep = createSampleTopic(topicIdToKeep, "Topic 2", new ArrayList<>());
        Exam exam = createSampleExam(examId, teacherId, ExamStatus.BORRADOR, List.of(topicToAdd, topicToKeep));

        Mockito.when(userRepository.findByEmail("teacher@test.com")).thenReturn(Optional.of(teacher));
        Mockito.when(examRepository.findById(examId)).thenReturn(Optional.of(exam));
        Mockito.when(examRepository.save(Mockito.any(Exam.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        Exam result = examService.addQuestion("teacher@test.com", examId, topicIdToAdd, "Q", "A", new BigDecimal("5"));

        // Assert
        Assertions.assertNotNull(result);
        Assertions.assertEquals(2, result.getTopics().size());
    }

    @Test
    void removeTopic_withMultipleTopics_coversTernaryFalseBranch() {
        // Arrange
        UUID teacherId = UUID.randomUUID();
        UUID examId = UUID.randomUUID();
        UUID topicIdToRemove = UUID.randomUUID();
        UUID topicIdToKeep = UUID.randomUUID();
        User teacher = createSampleTeacher(teacherId, "teacher@test.com");
        ExamTopic topicToRemove = createSampleTopic(topicIdToRemove, "Topic 1", new ArrayList<>());
        ExamTopic topicToKeep = createSampleTopic(topicIdToKeep, "Topic 2", new ArrayList<>());
        Exam exam = createSampleExam(examId, teacherId, ExamStatus.BORRADOR, List.of(topicToRemove, topicToKeep));

        Mockito.when(userRepository.findByEmail("teacher@test.com")).thenReturn(Optional.of(teacher));
        Mockito.when(examRepository.findById(examId)).thenReturn(Optional.of(exam));
        Mockito.when(examRepository.save(Mockito.any(Exam.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        Exam result = examService.removeTopic("teacher@test.com", examId, topicIdToRemove);

        // Assert
        Assertions.assertNotNull(result);
        Assertions.assertEquals(1, result.getTopics().size());
        Assertions.assertEquals(topicIdToKeep, result.getTopics().get(0).getId());
    }

    @Test
    void updateTopic_withMultipleTopics_coversTernaryFalseBranch() {
        // Arrange
        UUID teacherId = UUID.randomUUID();
        UUID examId = UUID.randomUUID();
        UUID topicIdToUpdate = UUID.randomUUID();
        UUID topicIdToKeep = UUID.randomUUID();
        User teacher = createSampleTeacher(teacherId, "teacher@test.com");
        ExamTopic topicToUpdate = createSampleTopic(topicIdToUpdate, "Old Name", new ArrayList<>());
        ExamTopic topicToKeep = createSampleTopic(topicIdToKeep, "Topic 2", new ArrayList<>());
        Exam exam = createSampleExam(examId, teacherId, ExamStatus.BORRADOR, List.of(topicToUpdate, topicToKeep));

        Mockito.when(userRepository.findByEmail("teacher@test.com")).thenReturn(Optional.of(teacher));
        Mockito.when(examRepository.findById(examId)).thenReturn(Optional.of(exam));
        Mockito.when(examRepository.save(Mockito.any(Exam.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        Exam result = examService.updateTopic("teacher@test.com", examId, topicIdToUpdate, "New Name");

        // Assert
        Assertions.assertNotNull(result);
        Assertions.assertEquals(2, result.getTopics().size());
    }
}
