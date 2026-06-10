package com.seventest.infrastructure.persistence.entity;

import com.seventest.domain.model.AiGradingConfidence;
import com.seventest.domain.model.AiGradingSuggestionStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ai_grading_suggestions", indexes = {
        @Index(name = "idx_ai_suggestion_submission", columnList = "submission_id"),
        @Index(name = "idx_ai_suggestion_answer", columnList = "answer_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiGradingSuggestionEntity {
    @Id
    private UUID id;
    @Column(name = "job_id", nullable = false)
    private UUID jobId;
    @Column(name = "submission_id", nullable = false)
    private UUID submissionId;
    @Column(name = "answer_id", nullable = false)
    private UUID answerId;
    @Column(name = "question_id", nullable = false)
    private UUID questionId;
    @Column(name = "attempt_number", nullable = false)
    private int attemptNumber;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AiGradingSuggestionStatus status;
    @Column(name = "suggested_fraction", precision = 3, scale = 2)
    private BigDecimal suggestedFraction;
    @Column(name = "suggested_score", precision = 10, scale = 4)
    private BigDecimal suggestedScore;
    @Column(name = "suggested_comment", length = 3000)
    private String suggestedComment;
    @Column(name = "strengths_json", columnDefinition = "text")
    private String strengthsJson;
    @Column(name = "issues_json", columnDefinition = "text")
    private String issuesJson;
    @Column(name = "source_pages_json", columnDefinition = "text")
    private String sourcePagesJson;
    @Enumerated(EnumType.STRING)
    private AiGradingConfidence confidence;
    @Column(name = "requires_human_review", nullable = false)
    private boolean requiresHumanReview;
    @Column(name = "review_reason", length = 1000)
    private String reviewReason;
    @Column(name = "error_summary", length = 1000)
    private String errorSummary;
    @Column(nullable = false)
    private String model;
    @Column(name = "prompt_version", nullable = false)
    private String promptVersion;
    @Column(name = "material_version", nullable = false)
    private String materialVersion;
    @Column(name = "material_sha256", nullable = false, length = 64)
    private String materialSha256;
    @Column(name = "answer_hash", nullable = false, length = 64)
    private String answerHash;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "reviewed_at")
    private Instant reviewedAt;
    @Column(name = "reviewed_by_teacher_id")
    private UUID reviewedByTeacherId;
}
