package com.seventest.domain.model;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Builder(toBuilder = true)
public class ExamQuestion {
    private final UUID id;
    private final String prompt;
    private final String modelAnswer;
    private final BigDecimal points;
    private final int displayOrder;
}
