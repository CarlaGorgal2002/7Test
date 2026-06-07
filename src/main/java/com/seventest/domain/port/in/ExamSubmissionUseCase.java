package com.seventest.domain.port.in;

import com.seventest.domain.model.ExamSubmission;

import java.util.List;
import java.util.UUID;

public interface ExamSubmissionUseCase {
    ExamSubmission start(String studentEmail, UUID examId, UUID topicId);
    ExamSubmission saveAnswers(String studentEmail, UUID submissionId, List<AnswerUpdate> answers);
    ExamSubmission submit(String studentEmail, UUID submissionId);
    List<ExamSubmission> listForStudent(String studentEmail);
    List<ExamSubmission> listForTeacherExam(String teacherEmail, UUID examId);
    ExamSubmission findForTeacher(String teacherEmail, UUID submissionId);
    ExamSubmission grade(String teacherEmail, UUID submissionId, List<GradeUpdate> updates);

    record AnswerUpdate(UUID questionId, String answerText) {}
    record GradeUpdate(UUID questionId, java.math.BigDecimal score, String comment, Boolean correctionIncorrect) {
        public GradeUpdate(UUID questionId, java.math.BigDecimal score, String comment) {
            this(questionId, score, comment, false);
        }
    }
}
