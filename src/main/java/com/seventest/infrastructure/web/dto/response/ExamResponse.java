package com.seventest.infrastructure.web.dto.response;

import com.seventest.domain.model.ExamStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ExamResponse(
        UUID id,
        String title,
        String description,
        String courseName,
        UUID teacherId,
        String teacherName,
        ExamStatus status,
        Instant availableFrom,
        Integer durationMinutes,
        List<ExamTopicResponse> topics,
        Instant createdAt,
        Instant updatedAt,
        Instant publishedAt
) {
}
