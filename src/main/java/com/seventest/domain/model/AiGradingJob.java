package com.seventest.domain.model;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder(toBuilder = true)
public class AiGradingJob {
    private final UUID id;
    private final UUID submissionId;
    private final UUID requestedByTeacherId;
    private final String requestedByTeacherEmail;
    private final AiGradingJobStatus status;
    private final int totalQuestions;
    private final int completedQuestions;
    private final int failedQuestions;
    private final String errorSummary;
    private final Instant createdAt;
    private final Instant startedAt;
    private final Instant completedAt;
}
