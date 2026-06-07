package com.seventest.infrastructure.persistence.adapter;

import com.seventest.domain.model.IncorrectCorrectionLog;
import com.seventest.domain.port.out.IncorrectCorrectionLogRepository;
import com.seventest.infrastructure.persistence.entity.IncorrectCorrectionLogEntity;
import com.seventest.infrastructure.persistence.repository.IncorrectCorrectionLogJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class IncorrectCorrectionLogRepositoryAdapter implements IncorrectCorrectionLogRepository {

    private final IncorrectCorrectionLogJpaRepository jpaRepository;

    @Override
    public IncorrectCorrectionLog save(IncorrectCorrectionLog log) {
        IncorrectCorrectionLogEntity entity = IncorrectCorrectionLogEntity.builder()
                .id(log.getId())
                .questionId(log.getQuestionId())
                .submissionId(log.getSubmissionId())
                .accuracyIa(log.getAccuracyIa())
                .scoreIa(log.getScoreIa())
                .scoreCorrected(log.getScoreCorrected())
                .timestamp(log.getTimestamp())
                .build();
        IncorrectCorrectionLogEntity saved = jpaRepository.save(entity);
        return IncorrectCorrectionLog.builder()
                .id(saved.getId())
                .questionId(saved.getQuestionId())
                .submissionId(saved.getSubmissionId())
                .accuracyIa(saved.getAccuracyIa())
                .scoreIa(saved.getScoreIa())
                .scoreCorrected(saved.getScoreCorrected())
                .timestamp(saved.getTimestamp())
                .build();
    }
}
