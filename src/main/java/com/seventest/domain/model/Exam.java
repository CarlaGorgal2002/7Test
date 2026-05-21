package com.seventest.domain.model;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Getter
@Builder(toBuilder = true)
public class Exam {
    private final UUID id;
    private final String title;
    private final String description;
    private final UUID teacherId;
    private final String teacherName;
    private final ExamStatus status;
    private final Instant availableFrom;
    private final Integer durationMinutes;
    private final List<ExamTopic> topics;
    private final Instant createdAt;
    private final Instant updatedAt;
    private final Instant publishedAt;
}
