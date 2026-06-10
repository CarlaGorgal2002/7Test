package com.seventest.domain.port.in;

import com.seventest.domain.model.AiGradingJob;
import com.seventest.domain.model.AiGradingStatus;
import com.seventest.domain.model.AiGradingSuggestion;

import java.util.List;
import java.util.UUID;

public interface AiCorrectionUseCase {
    AiGradingStatus status(String teacherEmail);
    AiGradingStatus checkStatus(String teacherEmail);
    AiGradingJob startJob(String teacherEmail, UUID submissionId);
    AiGradingJob findJob(String teacherEmail, UUID jobId);
    List<AiGradingSuggestion> listSuggestions(String teacherEmail, UUID submissionId);
    AiGradingSuggestion accept(String teacherEmail, UUID suggestionId);
    AiGradingSuggestion reject(String teacherEmail, UUID suggestionId);
}
