package com.seventest.domain.port.in;

import com.seventest.domain.model.Exam;
import com.seventest.domain.model.ExamQuestion;
import com.seventest.domain.model.ExamStatus;
import com.seventest.domain.model.ExamTopic;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface ExamManagementUseCase {
    Exam create(String teacherEmail, String title, String description, String courseName, Instant availableFrom, Integer durationMinutes);
    Exam update(String teacherEmail, UUID examId, String title, String description, String courseName, Instant availableFrom, Integer durationMinutes);
    Exam addTopic(String teacherEmail, UUID examId, String name);
    Exam updateTopic(String teacherEmail, UUID examId, UUID topicId, String name);
    Exam removeTopic(String teacherEmail, UUID examId, UUID topicId);
    Exam addQuestion(String teacherEmail, UUID examId, UUID topicId, String prompt, String modelAnswer, BigDecimal points);
    Exam updateQuestion(String teacherEmail, UUID examId, UUID topicId, UUID questionId, String prompt, String modelAnswer, BigDecimal points);
    Exam removeQuestion(String teacherEmail, UUID examId, UUID topicId, UUID questionId);
    Exam publish(String teacherEmail, UUID examId);
    Exam close(String teacherEmail, UUID examId);
    List<Exam> listForTeacher(String teacherEmail);
    List<Exam> listForSupervision(ExamStatus status);
    List<Exam> listPublishedForStudents();
    Exam findById(UUID examId);
    Exam publishFeedback(String teacherEmail, UUID examId);
    Exam addExtraTime(String teacherEmail, UUID examId, int extraMinutes);
    void deleteExam(String teacherEmail, UUID examId);
    void regrade(String teacherEmail, UUID examId);
}
