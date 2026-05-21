package com.seventest.infrastructure.persistence.adapter;

import com.seventest.domain.model.ExamSubmission;
import com.seventest.domain.port.out.ExamSubmissionRepository;
import com.seventest.infrastructure.persistence.mapper.ExamSubmissionMapper;
import com.seventest.infrastructure.persistence.repository.ExamSubmissionJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ExamSubmissionRepositoryAdapter implements ExamSubmissionRepository {

    private final ExamSubmissionJpaRepository jpaRepository;

    @Override
    public ExamSubmission save(ExamSubmission submission) {
        var entity = jpaRepository.findById(submission.getId())
                .map(existing -> {
                    ExamSubmissionMapper.updateEntity(existing, submission);
                    return existing;
                })
                .orElseGet(() -> ExamSubmissionMapper.toEntity(submission));
        return ExamSubmissionMapper.toDomain(jpaRepository.save(entity));
    }

    @Override
    public Optional<ExamSubmission> findById(UUID id) {
        return jpaRepository.findById(id).map(ExamSubmissionMapper::toDomain);
    }

    @Override
    public Optional<ExamSubmission> findByStudentIdAndExamId(UUID studentId, UUID examId) {
        return jpaRepository.findByStudentIdAndExamId(studentId, examId).map(ExamSubmissionMapper::toDomain);
    }

    @Override
    public List<ExamSubmission> findByStudentId(UUID studentId) {
        return jpaRepository.findByStudentIdOrderByUpdatedAtDesc(studentId).stream()
                .map(ExamSubmissionMapper::toDomain)
                .toList();
    }

    @Override
    public List<ExamSubmission> findByExamId(UUID examId) {
        return jpaRepository.findByExamIdOrderByUpdatedAtDesc(examId).stream()
                .map(ExamSubmissionMapper::toDomain)
                .toList();
    }
}
