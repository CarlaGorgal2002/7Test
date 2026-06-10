package com.seventest.infrastructure.web.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

public record ExamQuestionResponse(
        UUID id,
        String prompt,
        String modelAnswer,
        String teacherCriteria,
        BigDecimal points,
        int displayOrder
) {
}
