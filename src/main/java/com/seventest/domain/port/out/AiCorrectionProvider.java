package com.seventest.domain.port.out;

import com.seventest.domain.model.AiGradingConfidence;

import java.math.BigDecimal;
import java.util.List;

public interface AiCorrectionProvider {
    Result evaluate(Request request);
    Availability checkAvailability();

    record Availability(boolean available, String message) {
    }

    record Request(
            String questionType,
            String prompt,
            String modelAnswer,
            String teacherCriteria,
            String studentAnswer,
            String structuralDiagnostics
    ) {
    }

    record Result(
            BigDecimal suggestedFraction,
            String suggestedComment,
            List<String> strengths,
            List<String> issues,
            List<Integer> sourcePages,
            AiGradingConfidence confidence,
            boolean requiresHumanReview,
            String reviewReason
    ) {
    }
}
