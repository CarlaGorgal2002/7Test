package com.seventest.infrastructure.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.seventest.domain.model.*;
import com.seventest.domain.port.out.*;
import com.seventest.infrastructure.config.AppProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class AiGradingWorker implements AiGradingJobDispatcher, ApplicationListener<ApplicationReadyEvent> {
    private static final Set<BigDecimal> ALLOWED_FRACTIONS = Set.of(
            new BigDecimal("0.00"), new BigDecimal("0.25"), new BigDecimal("0.50"),
            new BigDecimal("0.75"), new BigDecimal("1.00"));
    private static final String TABLE_PREFIX = "7TEST_DECISION_TABLE:";
    private static final String TREE_PREFIX = "7TEST_DECISION_TREE:";

    private final AiGradingJobRepository jobRepository;
    private final AiGradingSuggestionRepository suggestionRepository;
    private final ExamSubmissionRepository submissionRepository;
    private final ExamRepository examRepository;
    private final AiCorrectionProvider provider;
    private final AppProperties properties;
    private final ObjectMapper objectMapper;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        if (!properties.getAiGrading().isReady()) return;
        for (AiGradingJob running : jobRepository.findByStatus(AiGradingJobStatus.RUNNING)) {
            AiGradingJob queued = jobRepository.save(running.toBuilder().status(AiGradingJobStatus.QUEUED)
                    .startedAt(null).errorSummary("Reanudado luego de un reinicio").build());
            dispatch(queued.getId());
        }
        jobRepository.findByStatus(AiGradingJobStatus.QUEUED).forEach(job -> dispatch(job.getId()));
    }

    @Async
    @Override
    public void dispatch(UUID jobId) {
        AiGradingJob job = jobRepository.findById(jobId).orElse(null);
        if (job == null || job.getStatus() != AiGradingJobStatus.QUEUED) return;
        job = jobRepository.save(job.toBuilder().status(AiGradingJobStatus.RUNNING).startedAt(Instant.now()).build());
        try {
            process(job);
        } catch (Exception ex) {
            log.warn("Trabajo de correccion IA {} fallo: {}", jobId, ex.getClass().getSimpleName());
            jobRepository.save(job.toBuilder().status(AiGradingJobStatus.FAILED)
                    .errorSummary("No se pudo procesar la entrega").completedAt(Instant.now()).build());
        }
    }

    private void process(AiGradingJob initialJob) {
        ExamSubmission submission = submissionRepository.findById(initialJob.getSubmissionId())
                .orElseThrow(() -> new IllegalArgumentException("Entrega no encontrada"));
        Exam exam = examRepository.findById(submission.getExamId())
                .orElseThrow(() -> new IllegalArgumentException("Examen no encontrado"));
        if (!Objects.equals(exam.getTeacherId(), initialJob.getRequestedByTeacherId())) {
            throw new IllegalArgumentException("El trabajo pertenece a otro profesor");
        }
        if (submission.getStatus() != SubmissionStatus.ENTREGADO || exam.isFeedbackPublished()) {
            throw new IllegalArgumentException("Entrega no habilitada para correccion");
        }
        ExamTopic topic = exam.getTopics().stream().filter(t -> t.getId().equals(submission.getTopicId()))
                .findFirst().orElseThrow(() -> new IllegalArgumentException("Tema no encontrado"));
        Map<UUID, ExamQuestion> questions = new HashMap<>();
        topic.getQuestions().forEach(question -> questions.put(question.getId(), question));

        int completed = 0;
        int failed = 0;
        for (ExamAnswer answer : submission.getAnswers()) {
            ExamQuestion question = questions.get(answer.getQuestionId());
            try {
                if (question == null) throw new IllegalArgumentException("Pregunta no encontrada");
                suggestionRepository.save(createSuggestion(initialJob, submission, answer, question));
            } catch (Exception ex) {
                failed++;
                suggestionRepository.save(failedSuggestion(initialJob, submission, answer, ex));
            }
            completed++;
            jobRepository.save(initialJob.toBuilder().status(AiGradingJobStatus.RUNNING)
                    .completedQuestions(completed).failedQuestions(failed).startedAt(initialJob.getStartedAt()).build());
        }
        jobRepository.save(initialJob.toBuilder()
                .status(failed == 0 ? AiGradingJobStatus.COMPLETED : AiGradingJobStatus.PARTIAL_FAILURE)
                .completedQuestions(completed).failedQuestions(failed).completedAt(Instant.now()).build());
    }

    private AiGradingSuggestion createSuggestion(AiGradingJob job, ExamSubmission submission,
                                                  ExamAnswer answer, ExamQuestion question) {
        String answerText = answer.getAnswerText() == null ? "" : answer.getAnswerText().trim();
        AiCorrectionProvider.Result result;
        if (answerText.isBlank()) {
            result = new AiCorrectionProvider.Result(BigDecimal.ZERO, "La respuesta esta vacia.", List.of(),
                    List.of("No se entrego una respuesta."), List.of(), AiGradingConfidence.HIGH, false, "");
        } else {
            result = validate(provider.evaluate(new AiCorrectionProvider.Request(questionType(answerText),
                    question.getPrompt(), question.getModelAnswer(), question.getTeacherCriteria(), answerText,
                    structuralDiagnostics(answerText))));
        }
        BigDecimal score = question.getPoints().multiply(result.suggestedFraction()).stripTrailingZeros();
        boolean missingPages = !answerText.isBlank() && result.sourcePages().isEmpty();
        boolean humanReview = result.requiresHumanReview() || result.confidence() == AiGradingConfidence.LOW || missingPages;
        String reviewReason = missingPages && blank(result.reviewReason())
                ? "Gemini no indico paginas de respaldo" : safe(result.reviewReason(), 1000);
        AppProperties.AiGrading config = properties.getAiGrading();
        return AiGradingSuggestion.builder().id(UUID.randomUUID()).jobId(job.getId()).submissionId(submission.getId())
                .answerId(answer.getId()).questionId(question.getId())
                .attemptNumber(suggestionRepository.nextAttemptNumber(answer.getId())).status(AiGradingSuggestionStatus.READY)
                .suggestedFraction(result.suggestedFraction()).suggestedScore(score)
                .suggestedComment(safe(result.suggestedComment(), 3000)).strengths(result.strengths()).issues(result.issues())
                .sourcePages(result.sourcePages()).confidence(result.confidence()).requiresHumanReview(humanReview)
                .reviewReason(reviewReason).model(config.getModel()).promptVersion(config.getPromptVersion())
                .materialVersion(config.getMaterialVersion()).materialSha256(config.getMaterialSha256())
                .answerHash(hash(answerText)).createdAt(Instant.now()).build();
    }

    private AiGradingSuggestion failedSuggestion(AiGradingJob job, ExamSubmission submission, ExamAnswer answer, Exception ex) {
        AppProperties.AiGrading config = properties.getAiGrading();
        return AiGradingSuggestion.builder().id(UUID.randomUUID()).jobId(job.getId()).submissionId(submission.getId())
                .answerId(answer.getId()).questionId(answer.getQuestionId())
                .attemptNumber(suggestionRepository.nextAttemptNumber(answer.getId())).status(AiGradingSuggestionStatus.FAILED)
                .strengths(List.of()).issues(List.of()).sourcePages(List.of()).requiresHumanReview(true)
                .reviewReason("La pregunta requiere correccion manual").errorSummary(safeError(ex))
                .model(config.getModel()).promptVersion(config.getPromptVersion()).materialVersion(config.getMaterialVersion())
                .materialSha256(config.getMaterialSha256()).answerHash(hash(answer.getAnswerText())).createdAt(Instant.now()).build();
    }

    private AiCorrectionProvider.Result validate(AiCorrectionProvider.Result result) {
        if (result == null || result.suggestedFraction() == null || result.confidence() == null
                || result.suggestedComment() == null) {
            throw new IllegalArgumentException("Respuesta de IA incompleta");
        }
        BigDecimal fraction = result.suggestedFraction().setScale(2);
        if (!ALLOWED_FRACTIONS.contains(fraction)) throw new IllegalArgumentException("Fraccion de IA invalida");
        List<Integer> pages = result.sourcePages() == null ? List.of() : result.sourcePages();
        if (pages.stream().anyMatch(page -> page == null || page < 1 || page > 158)) {
            throw new IllegalArgumentException("Pagina de respaldo invalida");
        }
        return new AiCorrectionProvider.Result(fraction, result.suggestedComment(),
                result.strengths() == null ? List.of() : result.strengths(),
                result.issues() == null ? List.of() : result.issues(), pages, result.confidence(),
                result.requiresHumanReview(), result.reviewReason() == null ? "" : result.reviewReason());
    }

    private String questionType(String answer) {
        if (answer.startsWith(TABLE_PREFIX)) return "DECISION_TABLE";
        if (answer.startsWith(TREE_PREFIX)) return "DECISION_TREE";
        return "TEXT";
    }

    private String structuralDiagnostics(String answer) {
        try {
            if (answer.startsWith(TABLE_PREFIX)) {
                JsonNode root = objectMapper.readTree(answer.substring(TABLE_PREFIX.length()));
                int rows = root.path("rows").asInt();
                int cols = root.path("cols").asInt();
                int nonBlank = 0;
                for (JsonNode row : root.path("cells")) for (JsonNode cell : row)
                    if (!cell.asText("").isBlank()) nonBlank++;
                return "Tabla normalizada: filas=" + rows + ", columnas=" + cols + ", celdasNoVacias=" + nonBlank
                        + ", estructuraValida=" + (rows > 0 && cols > 0 && root.path("cells").isArray());
            }
            if (answer.startsWith(TREE_PREFIX)) {
                JsonNode root = objectMapper.readTree(answer.substring(TREE_PREFIX.length()));
                Set<String> nodes = new HashSet<>();
                root.path("nodes").forEach(n -> nodes.add(n.path("id").asText()));
                int invalidEdges = 0;
                Set<String> incoming = new HashSet<>();
                Set<String> outgoing = new HashSet<>();
                for (JsonNode edge : root.path("edges")) {
                    String from = edge.path("from").path("nodeId").asText();
                    String to = edge.path("to").path("nodeId").asText();
                    if (!nodes.contains(from) || !nodes.contains(to)) invalidEdges++;
                    incoming.add(to);
                    outgoing.add(from);
                }
                long roots = nodes.stream().filter(n -> !incoming.contains(n)).count();
                long terminals = nodes.stream().filter(n -> !outgoing.contains(n)).count();
                return "Arbol normalizado: nodos=" + nodes.size() + ", conexiones=" + root.path("edges").size()
                        + ", raices=" + roots + ", terminales=" + terminals + ", conexionesInvalidas=" + invalidEdges;
            }
            return "Respuesta de texto.";
        } catch (Exception ex) {
            return "Estructura invalida: no se pudo normalizar.";
        }
    }

    private String hash(String value) {
        try {
            byte[] bytes = MessageDigest.getInstance("SHA-256")
                    .digest((value == null ? "" : value).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes).toUpperCase(Locale.ROOT);
        } catch (Exception ex) {
            throw new IllegalStateException("No se pudo calcular el hash", ex);
        }
    }

    private String safeError(Exception ex) {
        return switch (ex.getClass().getSimpleName()) {
            case "IllegalArgumentException" -> safe(ex.getMessage(), 500);
            default -> "El proveedor de IA no pudo evaluar la respuesta";
        };
    }

    private String safe(String value, int max) {
        if (value == null) return "";
        return value.length() <= max ? value : value.substring(0, max);
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
