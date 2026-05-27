package com.seventest.domain.port.out;

import com.seventest.domain.model.Exam;
import com.seventest.domain.model.ExamStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ExamRepository {
    Exam save(Exam exam);
    Optional<Exam> findById(UUID id);
    List<Exam> findByTeacherId(UUID teacherId);
    List<Exam> findByStatus(ExamStatus status);
    List<Exam> findAll();
    void deleteById(UUID id);
}
