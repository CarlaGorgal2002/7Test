package com.seventest.infrastructure.persistence.mapper;

import com.seventest.domain.model.Exam;
import com.seventest.domain.model.ExamQuestion;
import com.seventest.domain.model.ExamTopic;
import com.seventest.infrastructure.persistence.entity.ExamEntity;
import com.seventest.infrastructure.persistence.entity.ExamQuestionEntity;
import com.seventest.infrastructure.persistence.entity.ExamTopicEntity;

import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;

public final class ExamMapper {
    private ExamMapper() {
    }

    public static Exam toDomain(ExamEntity entity) {
        return Exam.builder()
                .id(entity.getId())
                .title(entity.getTitle())
                .description(entity.getDescription())
                .teacherId(entity.getTeacherId())
                .teacherName(entity.getTeacherName())
                .status(entity.getStatus())
                .availableFrom(entity.getAvailableFrom())
                .durationMinutes(entity.getDurationMinutes())
                .topics(entity.getTopics().stream()
                        .sorted(Comparator.comparing(ExamTopicEntity::getName, String.CASE_INSENSITIVE_ORDER))
                        .map(ExamMapper::toDomain)
                        .toList())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .publishedAt(entity.getPublishedAt())
                .build();
    }

    public static ExamEntity toEntity(Exam exam) {
        ExamEntity entity = ExamEntity.builder()
                .id(exam.getId())
                .title(exam.getTitle())
                .description(exam.getDescription())
                .teacherId(exam.getTeacherId())
                .teacherName(exam.getTeacherName())
                .status(exam.getStatus())
                .availableFrom(exam.getAvailableFrom())
                .durationMinutes(exam.getDurationMinutes())
                .createdAt(exam.getCreatedAt())
                .updatedAt(exam.getUpdatedAt())
                .publishedAt(exam.getPublishedAt())
                .topics(new LinkedHashSet<>())
                .build();
        syncTopics(entity, exam.getTopics());
        return entity;
    }

    public static void updateEntity(ExamEntity entity, Exam exam) {
        entity.setTitle(exam.getTitle());
        entity.setDescription(exam.getDescription());
        entity.setTeacherId(exam.getTeacherId());
        entity.setTeacherName(exam.getTeacherName());
        entity.setStatus(exam.getStatus());
        entity.setAvailableFrom(exam.getAvailableFrom());
        entity.setDurationMinutes(exam.getDurationMinutes());
        entity.setCreatedAt(exam.getCreatedAt());
        entity.setUpdatedAt(exam.getUpdatedAt());
        entity.setPublishedAt(exam.getPublishedAt());
        entity.getTopics().clear();
        syncTopics(entity, exam.getTopics());
    }

    private static ExamTopic toDomain(ExamTopicEntity entity) {
        return ExamTopic.builder()
                .id(entity.getId())
                .name(entity.getName())
                .questions(entity.getQuestions().stream()
                        .sorted(Comparator.comparingInt(ExamQuestionEntity::getDisplayOrder))
                        .map(ExamMapper::toDomain)
                        .toList())
                .build();
    }

    private static ExamQuestion toDomain(ExamQuestionEntity entity) {
        return ExamQuestion.builder()
                .id(entity.getId())
                .prompt(entity.getPrompt())
                .modelAnswer(entity.getModelAnswer())
                .points(entity.getPoints())
                .displayOrder(entity.getDisplayOrder())
                .build();
    }

    private static void syncTopics(ExamEntity examEntity, List<ExamTopic> topics) {
        if (topics == null) {
            return;
        }
        for (ExamTopic topic : topics) {
            ExamTopicEntity topicEntity = ExamTopicEntity.builder()
                    .id(topic.getId())
                    .name(topic.getName())
                    .exam(examEntity)
                    .questions(new LinkedHashSet<>())
                    .build();
            syncQuestions(topicEntity, topic.getQuestions());
            examEntity.getTopics().add(topicEntity);
        }
    }

    private static void syncQuestions(ExamTopicEntity topicEntity, List<ExamQuestion> questions) {
        if (questions == null) {
            return;
        }
        for (ExamQuestion question : questions) {
            topicEntity.getQuestions().add(ExamQuestionEntity.builder()
                    .id(question.getId())
                    .prompt(question.getPrompt())
                    .modelAnswer(question.getModelAnswer())
                    .points(question.getPoints())
                    .displayOrder(question.getDisplayOrder())
                    .topic(topicEntity)
                    .build());
        }
    }
}
