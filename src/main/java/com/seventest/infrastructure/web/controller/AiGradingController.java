package com.seventest.infrastructure.web.controller;

import com.seventest.domain.model.AiGradingJob;
import com.seventest.domain.model.AiGradingStatus;
import com.seventest.domain.model.AiGradingSuggestion;
import com.seventest.domain.port.in.AiCorrectionUseCase;
import com.seventest.infrastructure.web.dto.response.AiGradingJobResponse;
import com.seventest.infrastructure.web.dto.response.AiGradingSuggestionResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

@Tag(name = "Correccion con IA", description = "Sugerencias tentativas y auditables para docentes.")
@SecurityRequirement(name = "Bearer Auth")
@RestController
@RequestMapping("/api/ai-grading")
@PreAuthorize("hasRole('PROFESOR')")
@RequiredArgsConstructor
public class AiGradingController {
    private final AiCorrectionUseCase useCase;

    @GetMapping("/status")
    public ResponseEntity<AiGradingStatus> status(Principal principal) {
        return ResponseEntity.ok(useCase.status(principal.getName()));
    }

    @PostMapping("/status/check")
    public ResponseEntity<AiGradingStatus> checkStatus(Principal principal) {
        return ResponseEntity.ok(useCase.checkStatus(principal.getName()));
    }

    @PostMapping("/submissions/{submissionId}/jobs")
    public ResponseEntity<AiGradingJobResponse> start(@PathVariable UUID submissionId, Principal principal) {
        return ResponseEntity.accepted().body(toResponse(useCase.startJob(principal.getName(), submissionId)));
    }

    @GetMapping("/jobs/{jobId}")
    public ResponseEntity<AiGradingJobResponse> job(@PathVariable UUID jobId, Principal principal) {
        return ResponseEntity.ok(toResponse(useCase.findJob(principal.getName(), jobId)));
    }

    @GetMapping("/submissions/{submissionId}/suggestions")
    public ResponseEntity<List<AiGradingSuggestionResponse>> suggestions(@PathVariable UUID submissionId, Principal principal) {
        return ResponseEntity.ok(useCase.listSuggestions(principal.getName(), submissionId).stream()
                .map(this::toResponse).toList());
    }

    @PostMapping("/suggestions/{suggestionId}/accept")
    public ResponseEntity<AiGradingSuggestionResponse> accept(@PathVariable UUID suggestionId, Principal principal) {
        return ResponseEntity.ok(toResponse(useCase.accept(principal.getName(), suggestionId)));
    }

    @PostMapping("/suggestions/{suggestionId}/reject")
    public ResponseEntity<AiGradingSuggestionResponse> reject(@PathVariable UUID suggestionId, Principal principal) {
        return ResponseEntity.ok(toResponse(useCase.reject(principal.getName(), suggestionId)));
    }

    private AiGradingJobResponse toResponse(AiGradingJob job) {
        return new AiGradingJobResponse(job.getId(), job.getSubmissionId(), job.getStatus(), job.getTotalQuestions(),
                job.getCompletedQuestions(), job.getFailedQuestions(), job.getErrorSummary(), job.getCreatedAt(),
                job.getStartedAt(), job.getCompletedAt());
    }

    private AiGradingSuggestionResponse toResponse(AiGradingSuggestion suggestion) {
        return new AiGradingSuggestionResponse(suggestion.getId(), suggestion.getJobId(), suggestion.getSubmissionId(),
                suggestion.getAnswerId(), suggestion.getQuestionId(), suggestion.getAttemptNumber(), suggestion.getStatus(),
                suggestion.getSuggestedFraction(), suggestion.getSuggestedScore(), suggestion.getSuggestedComment(),
                suggestion.getStrengths(), suggestion.getIssues(), suggestion.getSourcePages(), suggestion.getConfidence(),
                suggestion.isRequiresHumanReview(), suggestion.getReviewReason(), suggestion.getErrorSummary(),
                suggestion.getModel(), suggestion.getPromptVersion(), suggestion.getMaterialVersion(),
                suggestion.getMaterialSha256(), suggestion.getAnswerHash(), suggestion.getCreatedAt(), suggestion.getReviewedAt());
    }
}
