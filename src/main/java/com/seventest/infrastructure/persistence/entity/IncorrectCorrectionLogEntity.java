package com.seventest.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "incorrect_correction_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IncorrectCorrectionLogEntity {

    @Id
    private UUID id;

    @Column(name = "question_id", nullable = false)
    private UUID questionId;

    @Column(name = "submission_id", nullable = false)
    private UUID submissionId;

    @Column(name = "accuracy_ia", precision = 3, scale = 2)
    private BigDecimal accuracyIa;

    @Column(name = "score_ia", precision = 6, scale = 2)
    private BigDecimal scoreIa;

    @Column(name = "score_corrected", precision = 6, scale = 2, nullable = false)
    private BigDecimal scoreCorrected;

    @Column(name = "timestamp", nullable = false)
    private Instant timestamp;
}
