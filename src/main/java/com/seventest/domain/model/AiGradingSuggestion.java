package com.seventest.domain.model;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Getter
@Builder(toBuilder = true)
public class AiGradingSuggestion {
    private final UUID id;
    private final UUID jobId;
    private final UUID submissionId;
    private final UUID answerId;
    private final UUID questionId;
    private final int attemptNumber;
    private final AiGradingSuggestionStatus status;
    private final BigDecimal suggestedFraction;
    private final BigDecimal suggestedScore;
    private final String suggestedComment;
    private final List<String> strengths;
    private final List<String> issues;
    private final List<Integer> sourcePages;
    private final AiGradingConfidence confidence;
    private final boolean requiresHumanReview;
    private final String reviewReason;
    private final String errorSummary;
    private final String model;
    private final String promptVersion;
    private final String materialVersion;
    private final String materialSha256;
    private final String answerHash;
    private final Instant createdAt;
    private final Instant reviewedAt;
    private final UUID reviewedByTeacherId;
}
