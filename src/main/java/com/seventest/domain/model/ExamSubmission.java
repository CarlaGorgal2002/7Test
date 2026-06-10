package com.seventest.domain.model;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Getter
@Builder(toBuilder = true)
public class ExamSubmission {
    private final UUID id;
    private final UUID examId;
    private final String examTitle;
    private final UUID topicId;
    private final String topicName;
    private final UUID studentId;
    private final String studentName;
    private final SubmissionStatus status;
    private final List<ExamAnswer> answers;
    private final Instant startedAt;
    private final Instant updatedAt;
    private final Instant submittedAt;
}
