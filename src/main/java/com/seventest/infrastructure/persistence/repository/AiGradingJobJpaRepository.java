package com.seventest.infrastructure.persistence.repository;

import com.seventest.domain.model.AiGradingJobStatus;
import com.seventest.infrastructure.persistence.entity.AiGradingJobEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AiGradingJobJpaRepository extends JpaRepository<AiGradingJobEntity, UUID> {
    Optional<AiGradingJobEntity> findFirstBySubmissionIdAndStatusInOrderByCreatedAtDesc(
            UUID submissionId, Collection<AiGradingJobStatus> statuses);
    List<AiGradingJobEntity> findByStatusOrderByCreatedAtAsc(AiGradingJobStatus status);
}
