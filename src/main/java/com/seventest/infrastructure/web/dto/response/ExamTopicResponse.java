package com.seventest.infrastructure.web.dto.response;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record ExamTopicResponse(
        UUID id,
        String name,
        BigDecimal totalPoints,
        List<ExamQuestionResponse> questions
) {
}
