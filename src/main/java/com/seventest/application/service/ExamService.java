package com.seventest.application.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.seventest.domain.exception.ExamNotFoundException;
import com.seventest.domain.exception.UserNotFoundException;
import com.seventest.domain.model.Exam;
import com.seventest.domain.model.ExamQuestion;
import com.seventest.domain.model.ExamStatus;
import com.seventest.domain.model.ExamTopic;
import com.seventest.domain.model.ExamSubmission;
import com.seventest.domain.model.Role;
import com.seventest.domain.model.User;
import com.seventest.domain.port.in.ExamManagementUseCase;
import com.seventest.domain.port.out.ExamRepository;
import com.seventest.domain.port.out.ExamSubmissionRepository;
import com.seventest.domain.port.out.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ExamService implements ExamManagementUseCase {

    private static final BigDecimal REQUIRED_TOPIC_TOTAL = BigDecimal.TEN;
    private static final String DEFAULT_COURSE_NAME = "Testing de Aplicaciones";
    private static final String DECISION_TREE_PREFIX = "7TEST_DECISION_TREE:";
    private static final String DECISION_TABLE_PREFIX = "7TEST_DECISION_TABLE:";
    private static final String EMPTY_DECISION_TREE = "7TEST_DECISION_TREE:{\"nodes\":[],\"edges\":[]}";
    private static final String EMPTY_DECISION_TABLE = "7TEST_DECISION_TABLE:{\"rows\":2,\"cols\":2,\"cells\":[[\"\",\"\"],[\"\",\"\"]]}";
    private static final ObjectMapper EDITOR_JSON = new ObjectMapper();
    private static final List<String> TOPIC_COLORS = List.of(
            "#1956D8", "#16A34A", "#D97706", "#DC2626", "#7C3AED",
            "#EA580C", "#0891B2", "#65A30D", "#DB2777", "#0D9488"
    );

    private final ExamRepository examRepository;
    private final UserRepository userRepository;
    private final ExamSubmissionRepository submissionRepository;
    private final GeminiGradingService geminiGradingService;

    @Override
    public Exam create(String teacherEmail, String title, String description, String courseName, Instant availableFrom, Integer durationMinutes) {
        User teacher = requireTeacher(teacherEmail);
        Instant now = Instant.now();
        Exam exam = Exam.builder()
                .id(UUID.randomUUID())
                .title(cleanRequired(title, "El titulo del examen es obligatorio"))
                .description(cleanOptional(description))
                .courseName(cleanCourseName(courseName))
                .teacherId(teacher.getId())
                .teacherName(teacher.getFullName())
                .status(ExamStatus.BORRADOR)
                .availableFrom(availableFrom)
                .durationMinutes(durationMinutes)
                .topics(List.of())
                .createdAt(now)
                .updatedAt(now)
                .publishedAt(null)
                .build();
        return examRepository.save(exam);
    }

    @Override
    public Exam update(String teacherEmail, UUID examId, String title, String description, String courseName, Instant availableFrom, Integer durationMinutes) {
        Exam exam = requireEditableOwnedExam(teacherEmail, examId);
        return examRepository.save(exam.toBuilder()
                .title(cleanRequired(title, "El titulo del examen es obligatorio"))
                .description(cleanOptional(description))
                .courseName(cleanCourseName(courseName))
                .availableFrom(availableFrom)
                .durationMinutes(durationMinutes)
                .updatedAt(Instant.now())
                .build());
    }

    @Override
    public Exam addTopic(String teacherEmail, UUID examId, String name) {
        Exam exam = requireEditableOwnedExam(teacherEmail, examId);
        List<ExamTopic> topics = new ArrayList<>(safeTopics(exam));
        topics.add(ExamTopic.builder()
                .id(UUID.randomUUID())
                .name(cleanRequired(name, "El nombre del tema es obligatorio"))
                .colorHex(nextTopicColor(topics.size()))
                .questions(List.of())
                .build());
        return saveWithTopics(exam, topics);
    }

    @Override
    public Exam updateTopic(String teacherEmail, UUID examId, UUID topicId, String name) {
        Exam exam = requireEditableOwnedExam(teacherEmail, examId);
        List<ExamTopic> topics = safeTopics(exam).stream()
                .map(topic -> topic.getId().equals(topicId)
                        ? topic.toBuilder().name(cleanRequired(name, "El nombre del tema es obligatorio")).build()
                        : topic)
                .toList();
        ensureTopicExists(topics, topicId);
        return saveWithTopics(exam, topics);
    }

    @Override
    public Exam removeTopic(String teacherEmail, UUID examId, UUID topicId) {
        Exam exam = requireEditableOwnedExam(teacherEmail, examId);
        List<ExamTopic> topics = safeTopics(exam).stream()
                .filter(topic -> !topic.getId().equals(topicId))
                .toList();
        if (topics.size() == safeTopics(exam).size()) {
            throw new IllegalArgumentException("Tema no encontrado");
        }
        return saveWithTopics(exam, topics);
    }

    @Override
    public Exam addQuestion(String teacherEmail, UUID examId, UUID topicId, String prompt, String modelAnswer, BigDecimal points) {
        Exam exam = requireEditableOwnedExam(teacherEmail, examId);
        List<ExamTopic> topics = safeTopics(exam).stream()
                .map(topic -> topic.getId().equals(topicId)
                        ? topic.toBuilder().questions(addQuestion(topic, prompt, modelAnswer, points)).build()
                        : topic)
                .toList();
        ensureTopicExists(topics, topicId);
        return saveWithTopics(exam, topics);
    }

    @Override
    public Exam updateQuestion(String teacherEmail, UUID examId, UUID topicId, UUID questionId, String prompt, String modelAnswer, BigDecimal points) {
        Exam exam = requireEditableOwnedExam(teacherEmail, examId);
        List<ExamTopic> topics = safeTopics(exam).stream()
                .map(topic -> {
                    if (!topic.getId().equals(topicId)) {
                        return topic;
                    }
                    List<ExamQuestion> questions = safeQuestions(topic).stream()
                            .map(question -> question.getId().equals(questionId)
                                    ? question.toBuilder()
                                        .prompt(cleanOptional(prompt))
                                        .modelAnswer(cleanOptional(modelAnswer))
                                        .points(validPoints(points))
                                        .build()
                                    : question)
                            .toList();
                    ensureQuestionExists(questions, questionId);
                    return topic.toBuilder().questions(questions).build();
                })
                .toList();
        ensureTopicExists(topics, topicId);
        return saveWithTopics(exam, topics);
    }

    @Override
    public Exam removeQuestion(String teacherEmail, UUID examId, UUID topicId, UUID questionId) {
        Exam exam = requireEditableOwnedExam(teacherEmail, examId);
        List<ExamTopic> topics = safeTopics(exam).stream()
                .map(topic -> {
                    if (!topic.getId().equals(topicId)) {
                        return topic;
                    }
                    List<ExamQuestion> before = safeQuestions(topic);
                    List<ExamQuestion> after = before.stream()
                            .filter(question -> !question.getId().equals(questionId))
                            .toList();
                    if (after.size() == before.size()) {
                        throw new IllegalArgumentException("Pregunta no encontrada");
                    }
                    return topic.toBuilder().questions(reindex(after)).build();
                })
                .toList();
        ensureTopicExists(topics, topicId);
        return saveWithTopics(exam, topics);
    }

    @Override
    public Exam publish(String teacherEmail, UUID examId) {
        Exam exam = requireOwnedExam(teacherEmail, examId);
        validateReadyToPublish(exam);
        Instant now = Instant.now();
        return examRepository.save(exam.toBuilder()
                .status(ExamStatus.PUBLICADO)
                .updatedAt(now)
                .publishedAt(now)
                .build());
    }

    @Override
    public Exam close(String teacherEmail, UUID examId) {
        Exam exam = requireOwnedExam(teacherEmail, examId);
        if (exam.getStatus() == ExamStatus.CERRADO) {
            return exam;
        }
        Exam closedExam = examRepository.save(exam.toBuilder()
                .status(ExamStatus.CERRADO)
                .updatedAt(Instant.now())
                .build());
        geminiGradingService.processExamSubmissions(examId);
        return closedExam;
    }

    @Override
    public void regrade(String teacherEmail, UUID examId) {
        requireOwnedExam(teacherEmail, examId);
        geminiGradingService.processExamSubmissions(examId);
    }

    @Override
    public List<Exam> listForTeacher(String teacherEmail) {
        User teacher = requireTeacher(teacherEmail);
        return examRepository.findByTeacherId(teacher.getId());
    }

    @Override
    public List<Exam> listForSupervision(ExamStatus status) {
        return status == null ? examRepository.findAll() : examRepository.findByStatus(status);
    }

    @Override
    public List<Exam> listPublishedForStudents() {
        List<Exam> result = new java.util.ArrayList<>();
        result.addAll(examRepository.findByStatus(ExamStatus.PUBLICADO));
        result.addAll(examRepository.findByStatus(ExamStatus.CERRADO));
        return result;
    }

    @Override
    public Exam findById(UUID examId) {
        return examRepository.findById(examId)
                .orElseThrow(() -> new ExamNotFoundException(examId));
    }

    @Override
    public Exam addExtraTime(String teacherEmail, UUID examId, int extraMinutes) {
        Exam exam = requireOwnedExam(teacherEmail, examId);
        if (exam.getStatus() != ExamStatus.PUBLICADO) {
            throw new IllegalArgumentException("Solo se puede agregar tiempo en un examen publicado");
        }
        if (exam.isExtraTimeUsed()) {
            throw new IllegalArgumentException("El tiempo extra solo puede usarse una vez");
        }
        if (extraMinutes < 1 || extraMinutes > 60) {
            throw new IllegalArgumentException("El tiempo extra debe ser entre 1 y 60 minutos");
        }
        int base = exam.getDurationMinutes() != null ? exam.getDurationMinutes() : 0;
        return examRepository.save(exam.toBuilder()
                .durationMinutes(base + extraMinutes)
                .extraTimeUsed(true)
                .updatedAt(Instant.now())
                .build());
    }

    @Override
    public Exam publishFeedback(String teacherEmail, UUID examId) {
        Exam exam = requireOwnedExam(teacherEmail, examId);
        if (exam.getStatus() == ExamStatus.BORRADOR) {
            throw new IllegalArgumentException("No se puede publicar devoluciones de un examen en borrador");
        }
        List<ExamSubmission> submissions = submissionRepository.findByExamId(examId);
        boolean allReviewed = submissions.stream().allMatch(ExamSubmission::isReviewed);
        if (!allReviewed) {
            throw new IllegalArgumentException("No se pueden publicar devoluciones porque hay entregas pendientes de revisión");
        }
        return examRepository.save(exam.toBuilder().feedbackPublished(true).updatedAt(Instant.now()).build());
    }

    @Override
    public void deleteExam(String teacherEmail, UUID examId) {
        Exam exam = requireOwnedExam(teacherEmail, examId);
        if (exam.getStatus() == ExamStatus.PUBLICADO) {
            throw new IllegalArgumentException("No se puede eliminar un examen publicado");
        }
        if (exam.getStatus() == ExamStatus.CERRADO && !submissionRepository.findByExamId(examId).isEmpty()) {
            throw new IllegalArgumentException("No se puede eliminar un examen cerrado con entregas registradas");
        }
        examRepository.deleteById(examId);
    }

    private Exam requireEditableOwnedExam(String teacherEmail, UUID examId) {
        Exam exam = requireOwnedExam(teacherEmail, examId);
        if (isEligibleForEdit(exam.getStatus())) {
            throw new IllegalArgumentException("Solo se puede editar un examen en borrador");
        }
        return exam;
    }

    private Exam requireOwnedExam(String teacherEmail, UUID examId) {
        User teacher = requireTeacher(teacherEmail);
        Exam exam = findById(examId);
        if (!exam.getTeacherId().equals(teacher.getId())) {
            throw new IllegalArgumentException("El examen pertenece a otro profesor");
        }
        return exam;
    }

    private User requireTeacher(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException(UUID.fromString("00000000-0000-0000-0000-000000000000")));
        if (user.getRole() != Role.PROFESOR) {
            throw new IllegalArgumentException("Solo un profesor puede gestionar examenes");
        }
        return user;
    }

    private Exam saveWithTopics(Exam exam, List<ExamTopic> topics) {
        return examRepository.save(exam.toBuilder()
                .topics(topics)
                .updatedAt(Instant.now())
                .build());
    }

    private List<ExamQuestion> addQuestion(ExamTopic topic, String prompt, String modelAnswer, BigDecimal points) {
        List<ExamQuestion> questions = new ArrayList<>(safeQuestions(topic));
        questions.add(ExamQuestion.builder()
                .id(UUID.randomUUID())
                .prompt(cleanOptional(prompt))
                .modelAnswer(cleanOptional(modelAnswer))
                .points(validPoints(points))
                .displayOrder(questions.size() + 1)
                .build());
        return questions;
    }

    private void validateReadyToPublish(Exam exam) {
        if (safeTopics(exam).isEmpty()) {
            throw new IllegalArgumentException("El examen debe tener al menos un tema");
        }
        for (ExamTopic topic : safeTopics(exam)) {
            if (safeQuestions(topic).isEmpty()) {
                throw new IllegalArgumentException("Cada tema debe tener al menos una pregunta");
            }
            if (topic.totalPoints().compareTo(REQUIRED_TOPIC_TOTAL) != 0) {
                throw new IllegalArgumentException("Cada tema debe sumar exactamente 10 puntos");
            }
            for (ExamQuestion question : safeQuestions(topic)) {
                String loc = topic.getName() + " · Pregunta " + question.getDisplayOrder();
                String hint = (question.getPrompt() != null && !question.getPrompt().isBlank())
                        ? ": " + question.getPrompt().substring(0, Math.min(60, question.getPrompt().length()))
                        : "";
                if (isBlankQuestionContent(question.getPrompt())) {
                    throw new IllegalArgumentException("Falta enunciado en " + loc);
                }
                if (isBlankQuestionContent(question.getModelAnswer())) {
                    throw new IllegalArgumentException("Falta respuesta modelo en " + loc + hint);
                }
            }
        }
    }

    private boolean isBlankQuestionContent(String value) {
        if (value == null || value.isBlank()) {
            return true;
        }
        String clean = value.trim();
        return EMPTY_DECISION_TREE.equals(clean)
                || EMPTY_DECISION_TABLE.equals(clean)
                || (clean.startsWith(DECISION_TREE_PREFIX) && !hasNonBlankTreeText(clean))
                || (clean.startsWith(DECISION_TABLE_PREFIX) && !hasNonBlankTableCell(clean));
    }

    private boolean hasNonBlankTreeText(String value) {
        try {
            JsonNode root = EDITOR_JSON.readTree(value.substring(DECISION_TREE_PREFIX.length()));
            for (JsonNode node : root.path("nodes")) {
                if (!node.path("text").asText("").isBlank()) {
                    return true;
                }
            }
            for (JsonNode edge : root.path("edges")) {
                if (!edge.path("label").asText("").isBlank()) {
                    return true;
                }
            }
            return false;
        } catch (Exception ex) {
            return false;
        }
    }

    private boolean hasNonBlankTableCell(String value) {
        try {
            List<JsonNode> rows = new ArrayList<>();
            EDITOR_JSON.readTree(value.substring(DECISION_TABLE_PREFIX.length())).path("cells").forEach(rows::add);
            for (JsonNode row : rows) {
                for (JsonNode cell : row) {
                    if (!cell.asText("").isBlank()) {
                        return true;
                    }
                }
            }
            return false;
        } catch (Exception ex) {
            return false;
        }
    }


    private BigDecimal validPoints(BigDecimal points) {
        if (points == null || points.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("El puntaje debe ser mayor a cero");
        }
        if (points.compareTo(REQUIRED_TOPIC_TOTAL) > 0) {
            throw new IllegalArgumentException("Una pregunta no puede valer mas de 10 puntos");
        }
        return points.stripTrailingZeros();
    }

    private String cleanRequired(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    private String cleanOptional(String value) {
        return value == null ? "" : value.trim();
    }

    private String cleanCourseName(String value) {
        String clean = cleanOptional(value);
        return clean.isBlank() ? DEFAULT_COURSE_NAME : clean;
    }

    private String nextTopicColor(int existingTopics) {
        return TOPIC_COLORS.get(existingTopics % TOPIC_COLORS.size());
    }

    private List<ExamTopic> safeTopics(Exam exam) {
        return exam.getTopics() == null ? List.of() : exam.getTopics();
    }

    private List<ExamQuestion> safeQuestions(ExamTopic topic) {
        return topic.getQuestions() == null
                ? List.of()
                : topic.getQuestions().stream()
                    .sorted(Comparator.comparingInt(ExamQuestion::getDisplayOrder))
                    .toList();
    }

    private List<ExamQuestion> reindex(List<ExamQuestion> questions) {
        List<ExamQuestion> indexed = new ArrayList<>();
        for (int i = 0; i < questions.size(); i++) {
            indexed.add(questions.get(i).toBuilder().displayOrder(i + 1).build());
        }
        return indexed;
    }

    /** Indica si el examen no esta disponible para recibir modificaciones. */
    private boolean isEligibleForEdit(ExamStatus status) {
        return status != ExamStatus.BORRADOR;
    }

    private void ensureTopicExists(List<ExamTopic> topics, UUID topicId) {
        if (topics.stream().noneMatch(topic -> topic.getId().equals(topicId))) {
            throw new IllegalArgumentException("Tema no encontrado");
        }
    }

    private void ensureQuestionExists(List<ExamQuestion> questions, UUID questionId) {
        if (questions.stream().noneMatch(question -> question.getId().equals(questionId))) {
            throw new IllegalArgumentException("Pregunta no encontrada");
        }
    }
}
