package com.seventest.infrastructure.web.dto.response;

import com.seventest.domain.model.AiGradingJobStatus;

import java.time.Instant;
import java.util.UUID;

public record AiGradingJobResponse(
        UUID id,
        UUID submissionId,
        AiGradingJobStatus status,
        int totalQuestions,
        int completedQuestions,
        int failedQuestions,
        String errorSummary,
        Instant createdAt,
        Instant startedAt,
        Instant completedAt
) {
}
