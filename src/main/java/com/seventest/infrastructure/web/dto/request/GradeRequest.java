package com.seventest.infrastructure.web.dto.request;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record GradeRequest(
        @NotNull List<QuestionGrade> answers
) {
    public record QuestionGrade(
            @NotNull UUID questionId,
            BigDecimal score,
            String comment,
            Boolean correctionIncorrect
    ) {}
}
