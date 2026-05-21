package com.seventest.application.service;

import com.seventest.domain.exception.ExamNotFoundException;
import com.seventest.domain.exception.ExamSubmissionNotFoundException;
import com.seventest.domain.exception.UserNotFoundException;
import com.seventest.domain.model.Exam;
import com.seventest.domain.model.ExamAnswer;
import com.seventest.domain.model.ExamQuestion;
import com.seventest.domain.model.ExamStatus;
import com.seventest.domain.model.ExamSubmission;
import com.seventest.domain.model.ExamTopic;
import com.seventest.domain.model.Role;
import com.seventest.domain.model.SubmissionStatus;
import com.seventest.domain.model.User;
import com.seventest.domain.port.in.ExamSubmissionUseCase;
import com.seventest.domain.port.out.ExamRepository;
import com.seventest.domain.port.out.ExamSubmissionRepository;
import com.seventest.domain.port.out.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExamSubmissionService implements ExamSubmissionUseCase {

    private final ExamSubmissionRepository submissionRepository;
    private final ExamRepository examRepository;
    private final UserRepository userRepository;

    @Override
    public ExamSubmission start(String studentEmail, UUID examId) {
        User student = requireRole(studentEmail, Role.ALUMNO, "Solo un alumno puede rendir examenes");
        Exam exam = requireExam(examId);
        if (exam.getStatus() != ExamStatus.PUBLICADO) {
            throw new IllegalArgumentException("El examen no esta publicado");
        }

        return submissionRepository.findByStudentIdAndExamId(student.getId(), examId)
                .orElseGet(() -> createSubmission(student, exam));
    }

    @Override
    public ExamSubmission saveAnswers(String studentEmail, UUID submissionId, List<AnswerUpdate> updates) {
        User student = requireRole(studentEmail, Role.ALUMNO, "Solo un alumno puede guardar respuestas");
        ExamSubmission submission = requireOwnedSubmission(student, submissionId);
        if (submission.getStatus() != SubmissionStatus.EN_PROGRESO) {
            throw new IllegalArgumentException("No se puede modificar una entrega finalizada");
        }

        ExamTopic topic = requireAssignedTopic(requireExam(submission.getExamId()), submission.getTopicId());
        Map<UUID, String> validQuestionIds = topic.getQuestions().stream()
                .collect(Collectors.toMap(ExamQuestion::getId, ExamQuestion::getPrompt));

        Instant now = Instant.now();
        Map<UUID, String> updateMap = updates == null ? Map.of() : updates.stream()
                .collect(Collectors.toMap(AnswerUpdate::questionId, update -> cleanAnswer(update.answerText()), (a, b) -> b));

        updateMap.keySet().forEach(questionId -> {
            if (!validQuestionIds.containsKey(questionId)) {
                throw new IllegalArgumentException("La pregunta no pertenece al tema asignado");
            }
        });

        List<ExamAnswer> answers = submission.getAnswers().stream()
                .map(answer -> updateMap.containsKey(answer.getQuestionId())
                        ? answer.toBuilder().answerText(updateMap.get(answer.getQuestionId())).updatedAt(now).build()
                        : answer)
                .toList();

        return submissionRepository.save(submission.toBuilder()
                .answers(answers)
                .updatedAt(now)
                .build());
    }

    @Override
    public ExamSubmission submit(String studentEmail, UUID submissionId) {
        User student = requireRole(studentEmail, Role.ALUMNO, "Solo un alumno puede entregar examenes");
        ExamSubmission submission = requireOwnedSubmission(student, submissionId);
        if (submission.getStatus() == SubmissionStatus.ENTREGADO) {
            return submission;
        }
        Instant now = Instant.now();
        return submissionRepository.save(submission.toBuilder()
                .status(SubmissionStatus.ENTREGADO)
                .updatedAt(now)
                .submittedAt(now)
                .build());
    }

    @Override
    public List<ExamSubmission> listForStudent(String studentEmail) {
        User student = requireRole(studentEmail, Role.ALUMNO, "Solo un alumno puede consultar sus entregas");
        return submissionRepository.findByStudentId(student.getId());
    }

    @Override
    public List<ExamSubmission> listForTeacherExam(String teacherEmail, UUID examId) {
        User teacher = requireRole(teacherEmail, Role.PROFESOR, "Solo un profesor puede consultar entregas");
        Exam exam = requireExam(examId);
        if (!exam.getTeacherId().equals(teacher.getId())) {
            throw new IllegalArgumentException("El examen pertenece a otro profesor");
        }
        return submissionRepository.findByExamId(examId);
    }

    private ExamSubmission createSubmission(User student, Exam exam) {
        ExamTopic topic = chooseTopicForStudent(exam, student);
        Instant now = Instant.now();
        List<ExamAnswer> answers = topic.getQuestions().stream()
                .sorted(Comparator.comparingInt(ExamQuestion::getDisplayOrder))
                .map(question -> ExamAnswer.builder()
                        .id(UUID.randomUUID())
                        .questionId(question.getId())
                        .answerText("")
                        .updatedAt(now)
                        .build())
                .toList();

        return submissionRepository.save(ExamSubmission.builder()
                .id(UUID.randomUUID())
                .examId(exam.getId())
                .examTitle(exam.getTitle())
                .topicId(topic.getId())
                .topicName(topic.getName())
                .studentId(student.getId())
                .studentName(student.getFullName())
                .status(SubmissionStatus.EN_PROGRESO)
                .answers(answers)
                .startedAt(now)
                .updatedAt(now)
                .submittedAt(null)
                .build());
    }

    private ExamTopic chooseTopicForStudent(Exam exam, User student) {
        List<ExamTopic> topics = exam.getTopics();
        if (topics == null || topics.isEmpty()) {
            throw new IllegalArgumentException("El examen no tiene temas disponibles");
        }
        int index = Math.floorMod(student.getId().hashCode(), topics.size());
        return topics.get(index);
    }

    private ExamSubmission requireOwnedSubmission(User student, UUID submissionId) {
        ExamSubmission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new ExamSubmissionNotFoundException(submissionId));
        if (!submission.getStudentId().equals(student.getId())) {
            throw new IllegalArgumentException("La entrega pertenece a otro alumno");
        }
        return submission;
    }

    private Exam requireExam(UUID examId) {
        return examRepository.findById(examId)
                .orElseThrow(() -> new ExamNotFoundException(examId));
    }

    private ExamTopic requireAssignedTopic(Exam exam, UUID topicId) {
        return exam.getTopics().stream()
                .filter(topic -> topic.getId().equals(topicId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("El tema asignado ya no existe"));
    }

    private User requireRole(String email, Role role, String message) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException(UUID.fromString("00000000-0000-0000-0000-000000000000")));
        if (user.getRole() != role) {
            throw new IllegalArgumentException(message);
        }
        return user;
    }

    private String cleanAnswer(String value) {
        return value == null ? "" : value.trim();
    }
}
