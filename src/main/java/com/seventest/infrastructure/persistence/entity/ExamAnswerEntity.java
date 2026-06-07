package com.seventest.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "exam_answers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExamAnswerEntity {

    @Id
    private UUID id;

    @Column(name = "question_id", nullable = false)
    private UUID questionId;

    @Column(name = "answer_text", nullable = false, length = 20000)
    private String answerText;

    @Column(name = "score", precision = 6, scale = 2)
    private java.math.BigDecimal score;

    @Column(name = "comment", length = 3000)
    private String comment;

    @Column(name = "score_ia", precision = 6, scale = 2)
    private java.math.BigDecimal scoreIa;

    @Column(name = "accuracy_ia", precision = 3, scale = 2)
    private java.math.BigDecimal accuracyIa;

    @Column(name = "feedback_ia", length = 4000)
    private String feedbackIa;

    @Column(name = "grading_status", length = 50)
    private String gradingStatus;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "submission_id", nullable = false)
    private ExamSubmissionEntity submission;
}
