package com.seventest.infrastructure.persistence.adapter;

import com.seventest.domain.model.AiGradingJob;
import com.seventest.domain.model.AiGradingJobStatus;
import com.seventest.domain.port.out.AiGradingJobRepository;
import com.seventest.infrastructure.persistence.entity.AiGradingJobEntity;
import com.seventest.infrastructure.persistence.repository.AiGradingJobJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class AiGradingJobRepositoryAdapter implements AiGradingJobRepository {
    private final AiGradingJobJpaRepository repository;

    @Override
    public AiGradingJob save(AiGradingJob job) {
        return toDomain(repository.save(toEntity(job)));
    }

    @Override
    public Optional<AiGradingJob> findById(UUID id) {
        return repository.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<AiGradingJob> findActiveBySubmissionId(UUID submissionId) {
        return repository.findFirstBySubmissionIdAndStatusInOrderByCreatedAtDesc(
                submissionId, List.of(AiGradingJobStatus.QUEUED, AiGradingJobStatus.RUNNING)).map(this::toDomain);
    }

    @Override
    public List<AiGradingJob> findByStatus(AiGradingJobStatus status) {
        return repository.findByStatusOrderByCreatedAtAsc(status).stream().map(this::toDomain).toList();
    }

    private AiGradingJobEntity toEntity(AiGradingJob job) {
        AiGradingJobEntity entity = repository.findById(job.getId()).orElseGet(AiGradingJobEntity::new);
        entity.setId(job.getId());
        entity.setSubmissionId(job.getSubmissionId());
        entity.setRequestedByTeacherId(job.getRequestedByTeacherId());
        entity.setRequestedByTeacherEmail(job.getRequestedByTeacherEmail());
        entity.setStatus(job.getStatus());
        entity.setTotalQuestions(job.getTotalQuestions());
        entity.setCompletedQuestions(job.getCompletedQuestions());
        entity.setFailedQuestions(job.getFailedQuestions());
        entity.setErrorSummary(job.getErrorSummary());
        entity.setCreatedAt(job.getCreatedAt());
        entity.setStartedAt(job.getStartedAt());
        entity.setCompletedAt(job.getCompletedAt());
        return entity;
    }

    private AiGradingJob toDomain(AiGradingJobEntity entity) {
        return AiGradingJob.builder()
                .id(entity.getId()).submissionId(entity.getSubmissionId())
                .requestedByTeacherId(entity.getRequestedByTeacherId())
                .requestedByTeacherEmail(entity.getRequestedByTeacherEmail())
                .status(entity.getStatus()).totalQuestions(entity.getTotalQuestions())
                .completedQuestions(entity.getCompletedQuestions()).failedQuestions(entity.getFailedQuestions())
                .errorSummary(entity.getErrorSummary()).createdAt(entity.getCreatedAt())
                .startedAt(entity.getStartedAt()).completedAt(entity.getCompletedAt()).build();
    }
}
