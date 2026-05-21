package com.seventest.domain.port.out;

import com.seventest.domain.model.ExamSubmission;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ExamSubmissionRepository {
    ExamSubmission save(ExamSubmission submission);
    Optional<ExamSubmission> findById(UUID id);
    Optional<ExamSubmission> findByStudentIdAndExamId(UUID studentId, UUID examId);
    List<ExamSubmission> findByStudentId(UUID studentId);
    List<ExamSubmission> findByExamId(UUID examId);
}
