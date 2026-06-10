package com.seventest.infrastructure.web.dto.response;

import com.seventest.domain.model.AiGradingConfidence;
import com.seventest.domain.model.AiGradingSuggestionStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AiGradingSuggestionResponse(
        UUID id,
        UUID jobId,
        UUID submissionId,
        UUID answerId,
        UUID questionId,
        int attemptNumber,
        AiGradingSuggestionStatus status,
        BigDecimal suggestedFraction,
        BigDecimal suggestedScore,
        String suggestedComment,
        List<String> strengths,
        List<String> issues,
        List<Integer> sourcePages,
        AiGradingConfidence confidence,
        boolean requiresHumanReview,
        String reviewReason,
        String errorSummary,
        String model,
        String promptVersion,
        String materialVersion,
        String materialSha256,
        String answerHash,
        Instant createdAt,
        Instant reviewedAt
) {
}
