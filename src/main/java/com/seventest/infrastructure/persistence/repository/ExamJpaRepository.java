package com.seventest.infrastructure.persistence.repository;

import com.seventest.domain.model.ExamStatus;
import com.seventest.infrastructure.persistence.entity.ExamEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ExamJpaRepository extends JpaRepository<ExamEntity, UUID> {
    @Override
    @EntityGraph(attributePaths = {"topics", "topics.questions"})
    Optional<ExamEntity> findById(UUID id);

    @EntityGraph(attributePaths = {"topics", "topics.questions"})
    List<ExamEntity> findByTeacherIdOrderByUpdatedAtDesc(UUID teacherId);

    @EntityGraph(attributePaths = {"topics", "topics.questions"})
    List<ExamEntity> findByStatusOrderByUpdatedAtDesc(ExamStatus status);

    @Override
    @EntityGraph(attributePaths = {"topics", "topics.questions"})
    List<ExamEntity> findAll();
}
