package com.seventest.domain.port.out;

import com.seventest.domain.model.AiGradingJob;
import com.seventest.domain.model.AiGradingJobStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AiGradingJobRepository {
    AiGradingJob save(AiGradingJob job);
    Optional<AiGradingJob> findById(UUID id);
    Optional<AiGradingJob> findActiveBySubmissionId(UUID submissionId);
    List<AiGradingJob> findByStatus(AiGradingJobStatus status);
}
