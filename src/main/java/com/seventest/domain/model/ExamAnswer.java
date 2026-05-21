package com.seventest.domain.model;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder(toBuilder = true)
public class ExamAnswer {
    private final UUID id;
    private final UUID questionId;
    private final String answerText;
    private final Instant updatedAt;
}
