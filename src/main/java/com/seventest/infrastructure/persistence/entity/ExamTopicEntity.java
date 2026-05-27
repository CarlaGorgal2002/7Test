package com.seventest.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "exam_topics")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExamTopicEntity {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(name = "color_hex", nullable = false)
    private String colorHex;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "exam_id", nullable = false)
    private ExamEntity exam;

    @OneToMany(mappedBy = "topic", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<ExamQuestionEntity> questions = new LinkedHashSet<>();
}
