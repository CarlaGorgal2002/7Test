package com.seventest.domain.port.in;

import com.seventest.domain.model.ExamSubmission;

import java.util.List;
import java.util.UUID;

public interface ExamSubmissionUseCase {
    ExamSubmission start(String studentEmail, UUID examId);
    ExamSubmission saveAnswers(String studentEmail, UUID submissionId, List<AnswerUpdate> answers);
    ExamSubmission submit(String studentEmail, UUID submissionId);
    List<ExamSubmission> listForStudent(String studentEmail);
    List<ExamSubmission> listForTeacherExam(String teacherEmail, UUID examId);

    record AnswerUpdate(UUID questionId, String answerText) {
    }
}
