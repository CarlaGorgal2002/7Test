package com.seventest.infrastructure.persistence.entity;

import com.seventest.domain.model.SubmissionStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "exam_submissions",
        uniqueConstraints = @UniqueConstraint(columnNames = {"student_id", "exam_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExamSubmissionEntity {

    @Id
    private UUID id;

    @Version
    private Long version;

    @Column(name = "exam_id", nullable = false)
    private UUID examId;

    @Column(name = "exam_title", nullable = false)
    private String examTitle;

    @Column(name = "topic_id", nullable = false)
    private UUID topicId;

    @Column(name = "topic_name", nullable = false)
    private String topicName;

    @Column(name = "student_id", nullable = false)
    private UUID studentId;

    @Column(name = "student_name", nullable = false)
    private String studentName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SubmissionStatus status;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "submitted_at")
    private Instant submittedAt;

    @OneToMany(mappedBy = "submission", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<ExamAnswerEntity> answers = new LinkedHashSet<>();
}
