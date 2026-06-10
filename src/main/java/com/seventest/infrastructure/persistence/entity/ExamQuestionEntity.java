package com.seventest.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "exam_questions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExamQuestionEntity {

    @Id
    private UUID id;

    @Column(nullable = false, length = 4000)
    private String prompt;

    @Column(name = "model_answer", nullable = false, length = 20000)
    private String modelAnswer;

    @Column(name = "teacher_criteria", length = 4000)
    @Builder.Default
    private String teacherCriteria = "";

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal points;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "topic_id", nullable = false)
    private ExamTopicEntity topic;
}
