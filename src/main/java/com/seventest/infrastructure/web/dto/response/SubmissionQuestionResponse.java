package com.seventest.infrastructure.web.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record SubmissionQuestionResponse(
        UUID questionId,
        String prompt,
        BigDecimal points,
        int displayOrder,
        String answerText,
        Instant answerUpdatedAt,
        String interactionType,
        BigDecimal score,
        String comment
) {
}
