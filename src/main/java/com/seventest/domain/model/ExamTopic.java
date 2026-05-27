package com.seventest.domain.model;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Getter
@Builder(toBuilder = true)
public class ExamTopic {
    private final UUID id;
    private final String name;
    private final String colorHex;
    private final List<ExamQuestion> questions;

    public BigDecimal totalPoints() {
        return questions == null
                ? BigDecimal.ZERO
                : questions.stream()
                    .map(ExamQuestion::getPoints)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
