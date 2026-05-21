package com.seventest.infrastructure.persistence.repository;

import com.seventest.infrastructure.persistence.entity.ExamSubmissionEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ExamSubmissionJpaRepository extends JpaRepository<ExamSubmissionEntity, UUID> {
    @Override
    @EntityGraph(attributePaths = "answers")
    Optional<ExamSubmissionEntity> findById(UUID id);

    @EntityGraph(attributePaths = "answers")
    Optional<ExamSubmissionEntity> findByStudentIdAndExamId(UUID studentId, UUID examId);

    @EntityGraph(attributePaths = "answers")
    List<ExamSubmissionEntity> findByStudentIdOrderByUpdatedAtDesc(UUID studentId);

    @EntityGraph(attributePaths = "answers")
    List<ExamSubmissionEntity> findByExamIdOrderByUpdatedAtDesc(UUID examId);
}
