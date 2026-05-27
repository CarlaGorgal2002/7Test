package com.seventest.infrastructure.web.controller;

import com.seventest.domain.model.Exam;
import com.seventest.domain.model.ExamAnswer;
import com.seventest.domain.model.ExamQuestion;
import com.seventest.domain.model.ExamSubmission;
import com.seventest.domain.model.ExamTopic;
import com.seventest.domain.port.in.ExamManagementUseCase;
import com.seventest.domain.port.in.ExamSubmissionUseCase;
import com.seventest.infrastructure.web.dto.request.SaveAnswersRequest;
import com.seventest.infrastructure.web.dto.response.ExamSubmissionResponse;
import com.seventest.infrastructure.web.dto.response.SubmissionQuestionResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.text.Normalizer;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Tag(name = "Entregas de examen", description = "Inicio, guardado automatico y entrega final del examen.")
@SecurityRequirement(name = "Bearer Auth")
@RestController
@RequestMapping("/api/submissions")
@RequiredArgsConstructor
public class ExamSubmissionController {

    private static final String DECISION_TREE_PREFIX = "7TEST_DECISION_TREE:";
    private static final String DECISION_TABLE_PREFIX = "7TEST_DECISION_TABLE:";

    private final ExamSubmissionUseCase submissionUseCase;
    private final ExamManagementUseCase examManagementUseCase;

    @Operation(summary = "Iniciar examen publicado")
    @PreAuthorize("hasRole('ALUMNO')")
    @PostMapping("/exams/{examId}/start")
    public ResponseEntity<ExamSubmissionResponse> start(@PathVariable UUID examId, Principal principal) {
        return ResponseEntity.ok(toResponse(submissionUseCase.start(principal.getName(), examId)));
    }

    @Operation(summary = "Listar entregas del alumno autenticado")
    @PreAuthorize("hasRole('ALUMNO')")
    @GetMapping("/mine")
    public ResponseEntity<List<ExamSubmissionResponse>> mine(Principal principal) {
        return ResponseEntity.ok(submissionUseCase.listForStudent(principal.getName()).stream()
                .map(this::toResponse)
                .toList());
    }

    @Operation(summary = "Guardar respuestas del alumno")
    @PreAuthorize("hasRole('ALUMNO')")
    @PutMapping("/{submissionId}/answers")
    public ResponseEntity<ExamSubmissionResponse> saveAnswers(@PathVariable UUID submissionId,
                                                              @Valid @RequestBody SaveAnswersRequest request,
                                                              Principal principal) {
        List<ExamSubmissionUseCase.AnswerUpdate> updates = request.answers().stream()
                .map(answer -> new ExamSubmissionUseCase.AnswerUpdate(answer.questionId(), answer.answerText()))
                .toList();
        return ResponseEntity.ok(toResponse(submissionUseCase.saveAnswers(principal.getName(), submissionId, updates)));
    }

    @Operation(summary = "Entregar examen final")
    @PreAuthorize("hasRole('ALUMNO')")
    @PatchMapping("/{submissionId}/submit")
    public ResponseEntity<ExamSubmissionResponse> submit(@PathVariable UUID submissionId, Principal principal) {
        return ResponseEntity.ok(toResponse(submissionUseCase.submit(principal.getName(), submissionId)));
    }

    @Operation(summary = "Ver entregas de un examen del profesor")
    @PreAuthorize("hasRole('PROFESOR')")
    @GetMapping("/exams/{examId}")
    public ResponseEntity<List<ExamSubmissionResponse>> byExam(@PathVariable UUID examId, Principal principal) {
        return ResponseEntity.ok(submissionUseCase.listForTeacherExam(principal.getName(), examId).stream()
                .map(this::toResponse)
                .toList());
    }

    private ExamSubmissionResponse toResponse(ExamSubmission submission) {
        Exam exam = examManagementUseCase.findById(submission.getExamId());
        ExamTopic topic = exam.getTopics().stream()
                .filter(candidate -> candidate.getId().equals(submission.getTopicId()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("El tema asignado ya no existe"));

        Map<UUID, ExamAnswer> answersByQuestion = submission.getAnswers().stream()
                .collect(Collectors.toMap(ExamAnswer::getQuestionId, answer -> answer));

        List<SubmissionQuestionResponse> questions = topic.getQuestions().stream()
                .sorted(Comparator.comparingInt(ExamQuestion::getDisplayOrder))
                .map(question -> {
                    ExamAnswer answer = answersByQuestion.get(question.getId());
                    return new SubmissionQuestionResponse(
                            question.getId(),
                            question.getPrompt(),
                            question.getPoints(),
                            question.getDisplayOrder(),
                            answer == null ? "" : answer.getAnswerText(),
                            answer == null ? null : answer.getUpdatedAt(),
                            interactionType(question));
                })
                .toList();

        return new ExamSubmissionResponse(
                submission.getId(),
                submission.getExamId(),
                submission.getExamTitle(),
                submission.getTopicId(),
                submission.getTopicName(),
                submission.getStudentId(),
                submission.getStudentName(),
                submission.getStatus(),
                questions,
                submission.getStartedAt(),
                submission.getUpdatedAt(),
                submission.getSubmittedAt());
    }

    private String interactionType(ExamQuestion question) {
        if ((question.getModelAnswer() != null && question.getModelAnswer().startsWith(DECISION_TABLE_PREFIX))
                || normalize(question.getPrompt()).contains("tabla de decision")
                || (question.getPoints() != null && question.getPoints().intValue() == 2 && question.getDisplayOrder() == 7)) {
            return "DECISION_TABLE";
        }
        if ((question.getModelAnswer() != null && question.getModelAnswer().startsWith(DECISION_TREE_PREFIX))
                || normalize(question.getPrompt()).contains("arbol de decision")) {
            return "DECISION_TREE";
        }
        return "TEXT";
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase();
    }
}
