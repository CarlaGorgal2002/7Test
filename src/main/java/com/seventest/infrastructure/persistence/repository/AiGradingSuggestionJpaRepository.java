package com.seventest.infrastructure.persistence.repository;

import com.seventest.infrastructure.persistence.entity.AiGradingSuggestionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AiGradingSuggestionJpaRepository extends JpaRepository<AiGradingSuggestionEntity, UUID> {
    List<AiGradingSuggestionEntity> findBySubmissionIdOrderByCreatedAtDesc(UUID submissionId);
    List<AiGradingSuggestionEntity> findByAnswerIdOrderByCreatedAtDesc(UUID answerId);
    long countByAnswerId(UUID answerId);
}
