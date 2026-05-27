package com.seventest.infrastructure.persistence.adapter;

import com.seventest.domain.model.Exam;
import com.seventest.domain.model.ExamStatus;
import com.seventest.domain.port.out.ExamRepository;
import com.seventest.infrastructure.persistence.mapper.ExamMapper;
import com.seventest.infrastructure.persistence.repository.ExamJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ExamRepositoryAdapter implements ExamRepository {

    private final ExamJpaRepository jpaRepository;

    @Override
    public Exam save(Exam exam) {
        var entity = jpaRepository.findById(exam.getId())
                .map(existing -> {
                    ExamMapper.updateEntity(existing, exam);
                    return existing;
                })
                .orElseGet(() -> ExamMapper.toEntity(exam));
        return ExamMapper.toDomain(jpaRepository.save(entity));
    }

    @Override
    public Optional<Exam> findById(UUID id) {
        return jpaRepository.findById(id).map(ExamMapper::toDomain);
    }

    @Override
    public List<Exam> findByTeacherId(UUID teacherId) {
        return jpaRepository.findByTeacherIdOrderByUpdatedAtDesc(teacherId).stream()
                .map(ExamMapper::toDomain)
                .toList();
    }

    @Override
    public List<Exam> findByStatus(ExamStatus status) {
        return jpaRepository.findByStatusOrderByUpdatedAtDesc(status).stream()
                .map(ExamMapper::toDomain)
                .toList();
    }

    @Override
    public List<Exam> findAll() {
        return jpaRepository.findAll().stream()
                .map(ExamMapper::toDomain)
                .sorted(Comparator.comparing(Exam::getUpdatedAt).reversed())
                .toList();
    }

    @Override
    public void deleteById(UUID id) {
        jpaRepository.deleteById(id);
    }
}
