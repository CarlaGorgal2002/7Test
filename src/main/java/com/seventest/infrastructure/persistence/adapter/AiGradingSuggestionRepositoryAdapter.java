package com.seventest.infrastructure.persistence.adapter;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.seventest.domain.model.AiGradingSuggestion;
import com.seventest.domain.port.out.AiGradingSuggestionRepository;
import com.seventest.infrastructure.persistence.entity.AiGradingSuggestionEntity;
import com.seventest.infrastructure.persistence.repository.AiGradingSuggestionJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class AiGradingSuggestionRepositoryAdapter implements AiGradingSuggestionRepository {
    private final AiGradingSuggestionJpaRepository repository;
    private final ObjectMapper objectMapper;

    @Override
    public AiGradingSuggestion save(AiGradingSuggestion suggestion) {
        return toDomain(repository.save(toEntity(suggestion)));
    }

    @Override
    public Optional<AiGradingSuggestion> findById(UUID id) {
        return repository.findById(id).map(this::toDomain);
    }

    @Override
    public List<AiGradingSuggestion> findBySubmissionId(UUID submissionId) {
        return repository.findBySubmissionIdOrderByCreatedAtDesc(submissionId).stream().map(this::toDomain).toList();
    }

    @Override
    public List<AiGradingSuggestion> findByAnswerId(UUID answerId) {
        return repository.findByAnswerIdOrderByCreatedAtDesc(answerId).stream().map(this::toDomain).toList();
    }

    @Override
    public int nextAttemptNumber(UUID answerId) {
        return Math.toIntExact(repository.countByAnswerId(answerId) + 1);
    }

    private AiGradingSuggestionEntity toEntity(AiGradingSuggestion value) {
        AiGradingSuggestionEntity entity = repository.findById(value.getId()).orElseGet(AiGradingSuggestionEntity::new);
        entity.setId(value.getId());
        entity.setJobId(value.getJobId());
        entity.setSubmissionId(value.getSubmissionId());
        entity.setAnswerId(value.getAnswerId());
        entity.setQuestionId(value.getQuestionId());
        entity.setAttemptNumber(value.getAttemptNumber());
        entity.setStatus(value.getStatus());
        entity.setSuggestedFraction(value.getSuggestedFraction());
        entity.setSuggestedScore(value.getSuggestedScore());
        entity.setSuggestedComment(value.getSuggestedComment());
        entity.setStrengthsJson(write(value.getStrengths()));
        entity.setIssuesJson(write(value.getIssues()));
        entity.setSourcePagesJson(write(value.getSourcePages()));
        entity.setConfidence(value.getConfidence());
        entity.setRequiresHumanReview(value.isRequiresHumanReview());
        entity.setReviewReason(value.getReviewReason());
        entity.setErrorSummary(value.getErrorSummary());
        entity.setModel(value.getModel());
        entity.setPromptVersion(value.getPromptVersion());
        entity.setMaterialVersion(value.getMaterialVersion());
        entity.setMaterialSha256(value.getMaterialSha256());
        entity.setAnswerHash(value.getAnswerHash());
        entity.setCreatedAt(value.getCreatedAt());
        entity.setReviewedAt(value.getReviewedAt());
        entity.setReviewedByTeacherId(value.getReviewedByTeacherId());
        return entity;
    }

    private AiGradingSuggestion toDomain(AiGradingSuggestionEntity value) {
        return AiGradingSuggestion.builder()
                .id(value.getId()).jobId(value.getJobId()).submissionId(value.getSubmissionId())
                .answerId(value.getAnswerId()).questionId(value.getQuestionId()).attemptNumber(value.getAttemptNumber())
                .status(value.getStatus()).suggestedFraction(value.getSuggestedFraction()).suggestedScore(value.getSuggestedScore())
                .suggestedComment(value.getSuggestedComment()).strengths(readStrings(value.getStrengthsJson()))
                .issues(readStrings(value.getIssuesJson())).sourcePages(readIntegers(value.getSourcePagesJson()))
                .confidence(value.getConfidence()).requiresHumanReview(value.isRequiresHumanReview())
                .reviewReason(value.getReviewReason()).errorSummary(value.getErrorSummary()).model(value.getModel())
                .promptVersion(value.getPromptVersion()).materialVersion(value.getMaterialVersion())
                .materialSha256(value.getMaterialSha256()).answerHash(value.getAnswerHash()).createdAt(value.getCreatedAt())
                .reviewedAt(value.getReviewedAt()).reviewedByTeacherId(value.getReviewedByTeacherId()).build();
    }

    private String write(Object value) {
        try {
            return objectMapper.writeValueAsString(value == null ? List.of() : value);
        } catch (Exception ex) {
            throw new IllegalStateException("No se pudo serializar la sugerencia", ex);
        }
    }

    private List<String> readStrings(String value) {
        return read(value, new TypeReference<>() {});
    }

    private List<Integer> readIntegers(String value) {
        return read(value, new TypeReference<>() {});
    }

    private <T> List<T> read(String value, TypeReference<List<T>> type) {
        if (value == null || value.isBlank()) return List.of();
        try {
            return objectMapper.readValue(value, type);
        } catch (Exception ex) {
            throw new IllegalStateException("No se pudo leer la sugerencia", ex);
        }
    }
}
