package com.seventest.infrastructure.web.controller;

import com.seventest.domain.model.Exam;
import com.seventest.domain.model.ExamQuestion;
import com.seventest.domain.model.ExamStatus;
import com.seventest.domain.model.ExamTopic;
import com.seventest.domain.port.in.ExamManagementUseCase;
import com.seventest.infrastructure.web.dto.request.ExamQuestionRequest;
import com.seventest.infrastructure.web.dto.request.ExamRequest;
import com.seventest.infrastructure.web.dto.request.ExamTopicRequest;
import com.seventest.infrastructure.web.dto.response.ExamQuestionResponse;
import com.seventest.infrastructure.web.dto.response.ExamResponse;
import com.seventest.infrastructure.web.dto.response.ExamTopicResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.security.Principal;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Tag(name = "Examenes", description = "Creacion, publicacion y supervision de examenes.")
@SecurityRequirement(name = "Bearer Auth")
@RestController
@RequestMapping("/api/exams")
@RequiredArgsConstructor
public class ExamController {

    private final ExamManagementUseCase examManagementUseCase;

    @Operation(summary = "Crear examen en borrador")
    @PreAuthorize("hasRole('PROFESOR')")
    @PostMapping
    public ResponseEntity<ExamResponse> create(@Valid @RequestBody ExamRequest request, Principal principal) {
        Exam exam = examManagementUseCase.create(principal.getName(),
                request.title(), request.description(), request.courseName(), request.availableFrom(), request.durationMinutes());
        return ResponseEntity.created(URI.create("/api/exams/" + exam.getId())).body(toResponse(exam));
    }

    @Operation(summary = "Listar examenes del profesor autenticado")
    @PreAuthorize("hasRole('PROFESOR')")
    @GetMapping("/mine")
    public ResponseEntity<List<ExamResponse>> mine(Principal principal) {
        return ResponseEntity.ok(examManagementUseCase.listForTeacher(principal.getName()).stream()
                .map(this::toResponse)
                .toList());
    }

    @Operation(summary = "Editar datos generales de un examen en borrador")
    @PreAuthorize("hasRole('PROFESOR')")
    @PutMapping("/{examId}")
    public ResponseEntity<ExamResponse> update(@PathVariable UUID examId,
                                               @Valid @RequestBody ExamRequest request,
                                               Principal principal) {
        return ResponseEntity.ok(toResponse(examManagementUseCase.update(principal.getName(), examId,
                request.title(), request.description(), request.courseName(), request.availableFrom(), request.durationMinutes())));
    }

    @Operation(summary = "Agregar tema a un examen en borrador")
    @PreAuthorize("hasRole('PROFESOR')")
    @PostMapping("/{examId}/topics")
    public ResponseEntity<ExamResponse> addTopic(@PathVariable UUID examId,
                                                 @Valid @RequestBody ExamTopicRequest request,
                                                 Principal principal) {
        return ResponseEntity.ok(toResponse(examManagementUseCase.addTopic(principal.getName(), examId, request.name())));
    }

    @Operation(summary = "Editar tema")
    @PreAuthorize("hasRole('PROFESOR')")
    @PutMapping("/{examId}/topics/{topicId}")
    public ResponseEntity<ExamResponse> updateTopic(@PathVariable UUID examId,
                                                    @PathVariable UUID topicId,
                                                    @Valid @RequestBody ExamTopicRequest request,
                                                    Principal principal) {
        return ResponseEntity.ok(toResponse(examManagementUseCase.updateTopic(principal.getName(), examId, topicId, request.name())));
    }

    @Operation(summary = "Eliminar tema")
    @PreAuthorize("hasRole('PROFESOR')")
    @DeleteMapping("/{examId}/topics/{topicId}")
    public ResponseEntity<ExamResponse> removeTopic(@PathVariable UUID examId,
                                                    @PathVariable UUID topicId,
                                                    Principal principal) {
        return ResponseEntity.ok(toResponse(examManagementUseCase.removeTopic(principal.getName(), examId, topicId)));
    }

    @Operation(summary = "Agregar pregunta a un tema")
    @PreAuthorize("hasRole('PROFESOR')")
    @PostMapping("/{examId}/topics/{topicId}/questions")
    public ResponseEntity<ExamResponse> addQuestion(@PathVariable UUID examId,
                                                    @PathVariable UUID topicId,
                                                    @Valid @RequestBody ExamQuestionRequest request,
                                                    Principal principal) {
        return ResponseEntity.ok(toResponse(examManagementUseCase.addQuestion(principal.getName(), examId, topicId,
                request.prompt(), request.modelAnswer(), request.points())));
    }

    @Operation(summary = "Editar pregunta")
    @PreAuthorize("hasRole('PROFESOR')")
    @PutMapping("/{examId}/topics/{topicId}/questions/{questionId}")
    public ResponseEntity<ExamResponse> updateQuestion(@PathVariable UUID examId,
                                                       @PathVariable UUID topicId,
                                                       @PathVariable UUID questionId,
                                                       @Valid @RequestBody ExamQuestionRequest request,
                                                       Principal principal) {
        return ResponseEntity.ok(toResponse(examManagementUseCase.updateQuestion(principal.getName(), examId, topicId, questionId,
                request.prompt(), request.modelAnswer(), request.points())));
    }

    @Operation(summary = "Eliminar pregunta")
    @PreAuthorize("hasRole('PROFESOR')")
    @DeleteMapping("/{examId}/topics/{topicId}/questions/{questionId}")
    public ResponseEntity<ExamResponse> removeQuestion(@PathVariable UUID examId,
                                                       @PathVariable UUID topicId,
                                                       @PathVariable UUID questionId,
                                                       Principal principal) {
        return ResponseEntity.ok(toResponse(examManagementUseCase.removeQuestion(principal.getName(), examId, topicId, questionId)));
    }

    @Operation(summary = "Publicar examen")
    @PreAuthorize("hasRole('PROFESOR')")
    @PatchMapping("/{examId}/publish")
    public ResponseEntity<ExamResponse> publish(@PathVariable UUID examId, Principal principal) {
        return ResponseEntity.ok(toResponse(examManagementUseCase.publish(principal.getName(), examId)));
    }

    @Operation(summary = "Cerrar examen")
    @PreAuthorize("hasRole('PROFESOR')")
    @PatchMapping("/{examId}/close")
    public ResponseEntity<ExamResponse> close(@PathVariable UUID examId, Principal principal) {
        return ResponseEntity.ok(toResponse(examManagementUseCase.close(principal.getName(), examId)));
    }

    @Operation(summary = "Eliminar examen (borrador siempre; cerrado solo sin entregas)")
    @PreAuthorize("hasRole('PROFESOR')")
    @DeleteMapping("/{examId}")
    public ResponseEntity<Void> deleteExam(@PathVariable UUID examId, Principal principal) {
        examManagementUseCase.deleteExam(principal.getName(), examId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Supervisar examenes")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'DIRECTOR_DE_CATEDRA')")
    @GetMapping("/supervision")
    public ResponseEntity<List<ExamResponse>> supervision(@RequestParam(required = false) ExamStatus status) {
        return ResponseEntity.ok(examManagementUseCase.listForSupervision(status).stream()
                .map(this::toResponse)
                .toList());
    }

    @Operation(summary = "Listar examenes publicados para alumnos")
    @PreAuthorize("hasRole('ALUMNO')")
    @GetMapping("/published")
    public ResponseEntity<List<ExamResponse>> published() {
        return ResponseEntity.ok(examManagementUseCase.listPublishedForStudents().stream()
                .map(this::toResponse)
                .toList());
    }

    private ExamResponse toResponse(Exam exam) {
        return new ExamResponse(
                exam.getId(),
                exam.getTitle(),
                exam.getDescription(),
                exam.getCourseName(),
                exam.getTeacherId(),
                exam.getTeacherName(),
                exam.getStatus(),
                exam.getAvailableFrom(),
                exam.getDurationMinutes(),
                exam.getTopics() == null ? List.of() : exam.getTopics().stream()
                        .sorted(Comparator.comparing(ExamTopic::getName, String.CASE_INSENSITIVE_ORDER))
                        .map(this::toTopicResponse)
                        .toList(),
                exam.getCreatedAt(),
                exam.getUpdatedAt(),
                exam.getPublishedAt());
    }

    private ExamTopicResponse toTopicResponse(ExamTopic topic) {
        return new ExamTopicResponse(
                topic.getId(),
                topic.getName(),
                topic.getColorHex(),
                topic.totalPoints(),
                topic.getQuestions() == null ? List.of() : topic.getQuestions().stream()
                        .sorted(Comparator.comparingInt(ExamQuestion::getDisplayOrder))
                        .map(this::toQuestionResponse)
                        .toList());
    }

    private ExamQuestionResponse toQuestionResponse(ExamQuestion question) {
        return new ExamQuestionResponse(
                question.getId(),
                question.getPrompt(),
                question.getModelAnswer(),
                question.getPoints(),
                question.getDisplayOrder());
    }
}
