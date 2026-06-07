package com.seventest.infrastructure.web.dto.response;

import com.seventest.domain.model.SubmissionStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ExamSubmissionResponse(
        UUID id,
        UUID examId,
        String examTitle,
        UUID topicId,
        String topicName,
        UUID studentId,
        String studentName,
        SubmissionStatus status,
        List<SubmissionQuestionResponse> questions,
        BigDecimal finalScore,
        boolean reviewed,
        Instant startedAt,
        Instant updatedAt,
        Instant submittedAt,
        boolean feedbackPublished
) {
}
