package com.seventest.domain.port.out;

import com.seventest.domain.model.AiGradingSuggestion;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AiGradingSuggestionRepository {
    AiGradingSuggestion save(AiGradingSuggestion suggestion);
    Optional<AiGradingSuggestion> findById(UUID id);
    List<AiGradingSuggestion> findBySubmissionId(UUID submissionId);
    List<AiGradingSuggestion> findByAnswerId(UUID answerId);
    int nextAttemptNumber(UUID answerId);
}
