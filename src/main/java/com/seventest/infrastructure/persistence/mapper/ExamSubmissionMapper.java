package com.seventest.infrastructure.persistence.mapper;

import com.seventest.domain.model.ExamAnswer;
import com.seventest.domain.model.ExamSubmission;
import com.seventest.infrastructure.persistence.entity.ExamAnswerEntity;
import com.seventest.infrastructure.persistence.entity.ExamSubmissionEntity;

import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;

public final class ExamSubmissionMapper {
    private ExamSubmissionMapper() {
    }

    public static ExamSubmission toDomain(ExamSubmissionEntity entity) {
        return ExamSubmission.builder()
                .id(entity.getId())
                .examId(entity.getExamId())
                .examTitle(entity.getExamTitle())
                .topicId(entity.getTopicId())
                .topicName(entity.getTopicName())
                .studentId(entity.getStudentId())
                .studentName(entity.getStudentName())
                .status(entity.getStatus())
                .answers(entity.getAnswers().stream()
                        .sorted(Comparator.comparing(answer -> answer.getQuestionId().toString()))
                        .map(ExamSubmissionMapper::toDomain)
                        .toList())
                .startedAt(entity.getStartedAt())
                .updatedAt(entity.getUpdatedAt())
                .submittedAt(entity.getSubmittedAt())
                .build();
    }

    public static ExamSubmissionEntity toEntity(ExamSubmission submission) {
        ExamSubmissionEntity entity = ExamSubmissionEntity.builder()
                .id(submission.getId())
                .examId(submission.getExamId())
                .examTitle(submission.getExamTitle())
                .topicId(submission.getTopicId())
                .topicName(submission.getTopicName())
                .studentId(submission.getStudentId())
                .studentName(submission.getStudentName())
                .status(submission.getStatus())
                .startedAt(submission.getStartedAt())
                .updatedAt(submission.getUpdatedAt())
                .submittedAt(submission.getSubmittedAt())
                .answers(new LinkedHashSet<>())
                .build();
        syncAnswers(entity, submission.getAnswers());
        return entity;
    }

    public static void updateEntity(ExamSubmissionEntity entity, ExamSubmission submission) {
        entity.setExamTitle(submission.getExamTitle());
        entity.setTopicName(submission.getTopicName());
        entity.setStatus(submission.getStatus());
        entity.setUpdatedAt(submission.getUpdatedAt());
        entity.setSubmittedAt(submission.getSubmittedAt());
        entity.getAnswers().clear();
        syncAnswers(entity, submission.getAnswers());
    }

    private static ExamAnswer toDomain(ExamAnswerEntity entity) {
        return ExamAnswer.builder()
                .id(entity.getId())
                .questionId(entity.getQuestionId())
                .answerText(entity.getAnswerText())
                .score(entity.getScore())
                .comment(entity.getComment())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private static void syncAnswers(ExamSubmissionEntity entity, List<ExamAnswer> answers) {
        if (answers == null) {
            return;
        }
        for (ExamAnswer answer : answers) {
            entity.getAnswers().add(ExamAnswerEntity.builder()
                    .id(answer.getId())
                    .questionId(answer.getQuestionId())
                    .answerText(answer.getAnswerText())
                    .score(answer.getScore())
                    .comment(answer.getComment())
                    .updatedAt(answer.getUpdatedAt())
                    .submission(entity)
                    .build());
        }
    }
}
