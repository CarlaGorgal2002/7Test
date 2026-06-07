package com.seventest.domain.model;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Builder(toBuilder = true)
public class IncorrectCorrectionLog {
    private final UUID id;
    private final UUID questionId;
    private final UUID submissionId;
    private final BigDecimal accuracyIa;
    private final BigDecimal scoreIa;
    private final BigDecimal scoreCorrected;
    private final Instant timestamp;
}
