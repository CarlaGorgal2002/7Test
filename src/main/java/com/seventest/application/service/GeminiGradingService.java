package com.seventest.application.service;

import com.seventest.domain.model.Exam;
import com.seventest.domain.model.ExamAnswer;
import com.seventest.domain.model.ExamQuestion;
import com.seventest.domain.model.ExamSubmission;
import com.seventest.domain.model.ExamTopic;
import com.seventest.domain.port.out.ExamRepository;
import com.seventest.domain.port.out.ExamSubmissionRepository;
import com.seventest.infrastructure.gemini.GeminiClient;
import com.seventest.infrastructure.syllabus.SyllabusProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class GeminiGradingService {

    private final ExamRepository examRepository;
    private final ExamSubmissionRepository submissionRepository;
    private final GeminiClient geminiClient;
    private final SyllabusProvider syllabusProvider;

    private static final String DECISION_TREE_PREFIX = "7TEST_DECISION_TREE:";
    private static final String DECISION_TABLE_PREFIX = "7TEST_DECISION_TABLE:";

    @Async
    public void processExamSubmissions(UUID examId) {
        log.info("Starting asynchronous AI grading process for exam ID: {}", examId);
        try {
            Exam exam = examRepository.findById(examId).orElse(null);
            if (exam == null) {
                log.error("Exam not found for ID: {}", examId);
                return;
            }

            List<ExamSubmission> submissions = submissionRepository.findByExamId(examId);
            if (submissions.isEmpty()) {
                log.info("No submissions found for exam ID: {}", examId);
                return;
            }

            String syllabusContext = syllabusProvider.getSyllabus();

            for (ExamSubmission submission : submissions) {
                try {
                    processSubmission(exam, submission, syllabusContext);
                } catch (Exception e) {
                    log.error("Error processing submission ID: {}", submission.getId(), e);
                }
            }
            log.info("Completed AI grading process for exam ID: {}", examId);

        } catch (Exception e) {
            log.error("Failed to run AI grading process for exam ID: {}", examId, e);
        }
    }

    private void processSubmission(Exam exam, ExamSubmission submission, String syllabusContext) {
        ExamTopic topic = exam.getTopics().stream()
                .filter(t -> t.getId().equals(submission.getTopicId()))
                .findFirst()
                .orElse(null);

        if (topic == null) {
            log.error("Topic ID {} not found in exam ID {}", submission.getTopicId(), exam.getId());
            return;
        }

        Map<UUID, ExamQuestion> questions = topic.getQuestions().stream()
                .collect(Collectors.toMap(ExamQuestion::getId, q -> q));

        Instant now = Instant.now();
        List<ExamAnswer> updatedAnswers = new ArrayList<>();

        for (ExamAnswer answer : submission.getAnswers()) {
            ExamQuestion question = questions.get(answer.getQuestionId());
            if (question == null) {
                updatedAnswers.add(answer);
                continue;
            }

            if (isTextQuestion(question)) {
                try {
                    log.info("Grading question ID: {} for student: {}", question.getId(), submission.getStudentName());
                    String systemInstruction = buildSystemInstruction(syllabusContext);
                    String promptText = buildPromptText(question, answer);

                    GeminiClient.GradingResult result = geminiClient.evaluate(systemInstruction, promptText);

                    BigDecimal accuracy = result.getAccuracy();
                    // score = points * accuracy
                    BigDecimal maxPoints = question.getPoints() != null ? question.getPoints() : BigDecimal.ZERO;
                    BigDecimal score = maxPoints.multiply(accuracy).setScale(2, RoundingMode.HALF_UP);

                    updatedAnswers.add(answer.toBuilder()
                            .score(score)
                            .comment(result.getFeedback())
                            .scoreIa(score)
                            .accuracyIa(accuracy)
                            .feedbackIa(result.getFeedback())
                            .gradingStatus("IA_SUCCESS")
                            .updatedAt(now)
                            .build());

                } catch (Exception e) {
                    log.error("Failed to grade question ID {} for submission ID {}", question.getId(), submission.getId(), e);
                    updatedAnswers.add(answer.toBuilder()
                            .gradingStatus("IA_FAILED")
                            .updatedAt(now)
                            .build());
                }
            } else {
                // Las preguntas de árbol de decisión y tabla de decisión permanecen pendientes
                updatedAnswers.add(answer.toBuilder()
                        .gradingStatus("PENDING")
                        .build());
            }
        }

        // Calcular nota final sumando los puntajes obtenidos (ignorar nulos o tratarlos como 0)
        BigDecimal finalScore = BigDecimal.ZERO;
        for (ExamAnswer answer : updatedAnswers) {
            if (answer.getScore() != null) {
                finalScore = finalScore.add(answer.getScore());
            }
        }
        finalScore = finalScore.setScale(2, RoundingMode.HALF_UP);

        // reviewed = true si todas las preguntas tienen un puntaje asignado
        boolean reviewed = updatedAnswers.stream().allMatch(a -> a.getScore() != null);

        ExamSubmission updatedSubmission = submission.toBuilder()
                .answers(updatedAnswers)
                .finalScore(finalScore)
                .reviewed(reviewed)
                .updatedAt(now)
                .build();

        submissionRepository.save(updatedSubmission);
        log.info("Saved AI graded submission ID: {} for student: {} with score: {}", 
                submission.getId(), submission.getStudentName(), finalScore);
    }

    private boolean isTextQuestion(ExamQuestion question) {
        String answer = question.getModelAnswer();
        if (answer != null && (answer.startsWith(DECISION_TREE_PREFIX) || answer.startsWith(DECISION_TABLE_PREFIX))) {
            return false;
        }
        String prompt = question.getPrompt() != null ? question.getPrompt().toLowerCase() : "";
        if (prompt.contains("tabla de decision") || prompt.contains("arbol de decision")) {
            return false;
        }
        return true;
    }

    private String buildSystemInstruction(String syllabusContext) {
        return "Actuás como un evaluador docente titular y sumamente calificado para la materia de grado 'Testing de Aplicaciones' de la Universidad Argentina de la Empresa (UADE).\n"
                + "Tu objetivo es corregir y asignar una calificación parcial de exactitud conceptual a la respuesta del alumno para una pregunta teórica de texto libre, comparándola detalladamente contra la respuesta modelo provista por el profesor.\n"
                + "Debes basar rigurosamente tus criterios teóricos en los siguientes contenidos oficiales del syllabus de la materia:\n"
                + "-------\n"
                + syllabusContext + "\n"
                + "-------\n"
                + "Reglas estrictas de evaluación:\n"
                + "1. Asigna un nivel de exactitud o completitud ('accuracy') seleccionando estrictamente uno de los siguientes valores numéricos decimales: 0.0, 0.25, 0.5, 0.75, 1.0.\n"
                + "2. Si la respuesta del alumno es semánticamente equivalente a la respuesta modelo o demuestra un dominio completo y correcto de los conceptos (incluso con otras palabras válidas en español), asigna un accuracy de 1.0.\n"
                + "3. Si la respuesta está vacía, es incomprensible, no tiene relación alguna o es incorrecta en su totalidad, asigna un accuracy de 0.0.\n"
                + "4. Para respuestas parcialmente correctas, asigna 0.25, 0.5 o 0.75 según el nivel de completitud comparado con la respuesta modelo.\n"
                + "5. Escribe un comentario justificativo ('feedback') en español de Argentina, de tono constructivo, profesional y conciso (máximo 3 oraciones). Explica puntualmente qué conceptos clave faltaron o se expresaron de forma errónea. Si el accuracy es 1.0, el feedback debe ser nulo o vacío.\n"
                + "6. No menciones en tu devolución de feedback que eres un modelo de lenguaje de IA ni que la corrección fue automatizada. La devolución debe leerse como si fuera escrita por el profesor.";
    }

    private String buildPromptText(ExamQuestion question, ExamAnswer answer) {
        return "Enunciado de la Pregunta:\n"
                + question.getPrompt() + "\n\n"
                + "Respuesta Modelo del Profesor:\n"
                + question.getModelAnswer() + "\n\n"
                + "Respuesta entregada por el Alumno:\n"
                + (answer.getAnswerText() != null && !answer.getAnswerText().isBlank() ? answer.getAnswerText() : "(Sin responder)") + "\n\n"
                + "Puntaje máximo de la pregunta: " + question.getPoints() + " puntos.\n";
    }
}
