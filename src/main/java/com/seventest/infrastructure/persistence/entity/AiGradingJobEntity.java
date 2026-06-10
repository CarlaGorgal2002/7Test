package com.seventest.infrastructure.persistence.entity;

import com.seventest.domain.model.AiGradingJobStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ai_grading_jobs", indexes = {
        @Index(name = "idx_ai_job_submission", columnList = "submission_id"),
        @Index(name = "idx_ai_job_status_created", columnList = "status,created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiGradingJobEntity {
    @Id
    private UUID id;

    @Version
    private Long version;

    @Column(name = "submission_id", nullable = false)
    private UUID submissionId;

    @Column(name = "requested_by_teacher_id", nullable = false)
    private UUID requestedByTeacherId;

    @Column(name = "requested_by_teacher_email", nullable = false)
    private String requestedByTeacherEmail;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AiGradingJobStatus status;

    @Column(name = "total_questions", nullable = false)
    private int totalQuestions;

    @Column(name = "completed_questions", nullable = false)
    private int completedQuestions;

    @Column(name = "failed_questions", nullable = false)
    private int failedQuestions;

    @Column(name = "error_summary", length = 1000)
    private String errorSummary;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;
}
