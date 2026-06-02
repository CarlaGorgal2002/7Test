package com.seventest.infrastructure.persistence.entity;

import com.seventest.domain.model.ExamStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "exams")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExamEntity {

    @Id
    private UUID id;

    @Version
    private Long version;

    @Column(nullable = false)
    private String title;

    @Column(length = 2000)
    private String description;

    @Column(name = "course_name", nullable = false)
    private String courseName;

    @Column(name = "teacher_id", nullable = false)
    private UUID teacherId;

    @Column(name = "teacher_name", nullable = false)
    private String teacherName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ExamStatus status;

    @Column(name = "available_from")
    private Instant availableFrom;

    @Column(name = "duration_minutes")
    private Integer durationMinutes;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "feedback_published", nullable = false)
    @Builder.Default
    private boolean feedbackPublished = false;

    @Column(name = "extra_time_used", nullable = false)
    @Builder.Default
    private boolean extraTimeUsed = false;

    @OneToMany(mappedBy = "exam", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<ExamTopicEntity> topics = new LinkedHashSet<>();
}
