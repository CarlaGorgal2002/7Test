package com.seventest.application.service;

import com.seventest.domain.model.*;
import com.seventest.domain.port.in.AiCorrectionUseCase;
import com.seventest.domain.port.in.ExamManagementUseCase;
import com.seventest.domain.port.in.ExamSubmissionUseCase;
import com.seventest.domain.port.out.AiGradingJobDispatcher;
import com.seventest.domain.port.out.AiGradingJobRepository;
import com.seventest.domain.port.out.AiGradingSuggestionRepository;
import com.seventest.domain.port.out.AiCorrectionProvider;
import com.seventest.domain.port.out.UserRepository;
import com.seventest.infrastructure.config.AppProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AiCorrectionService implements AiCorrectionUseCase {
    private final AppProperties properties;
    private final ExamSubmissionUseCase submissionUseCase;
    private final ExamManagementUseCase examUseCase;
    private final UserRepository userRepository;
    private final AiGradingJobRepository jobRepository;
    private final AiGradingSuggestionRepository suggestionRepository;
    private final AiGradingJobDispatcher dispatcher;
    private final AiCorrectionProvider provider;

    @Override
    public AiGradingStatus status(String teacherEmail) {
        requireVipTeacher(teacherEmail);
        AppProperties.AiGrading config = properties.getAiGrading();
        return new AiGradingStatus(config.isEnabled(), config.isReady(), config.getModel(),
                config.getMaterialVersion(), config.getPromptVersion(),
                config.isReady() ? "OpenAI esta configurado; falta comprobar conectividad."
                        : "OpenAI no esta configurado. La correccion manual sigue disponible.");
    }

    @Override
    public AiGradingStatus checkStatus(String teacherEmail) {
        requireVipTeacher(teacherEmail);
        AppProperties.AiGrading config = properties.getAiGrading();
        if (!config.isReady()) return status(teacherEmail);
        AiCorrectionProvider.Availability availability = provider.checkAvailability();
        return new AiGradingStatus(config.isEnabled(), availability.available(), config.getModel(),
                config.getMaterialVersion(), config.getPromptVersion(), availability.message());
    }

    @Override
    public synchronized AiGradingJob startJob(String teacherEmail, UUID submissionId) {
        requireVipTeacher(teacherEmail);
        if (!properties.getAiGrading().isReady()) {
            throw new IllegalArgumentException("La correccion con IA no esta disponible");
        }
        ExamSubmission submission = requireGradableSubmission(teacherEmail, submissionId);
        var active = jobRepository.findActiveBySubmissionId(submissionId);
        if (active.isPresent()) return active.get();

        User teacher = requireTeacher(teacherEmail);
        AiGradingJob job = jobRepository.save(AiGradingJob.builder()
                .id(UUID.randomUUID()).submissionId(submissionId).requestedByTeacherId(teacher.getId())
                .requestedByTeacherEmail(teacher.getEmail()).status(AiGradingJobStatus.QUEUED)
                .totalQuestions(submission.getAnswers().size()).completedQuestions(0).failedQuestions(0)
                .createdAt(Instant.now()).build());
        dispatcher.dispatch(job.getId());
        return job;
    }

    @Override
    public AiGradingJob findJob(String teacherEmail, UUID jobId) {
        requireVipTeacher(teacherEmail);
        AiGradingJob job = jobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Trabajo de IA no encontrado"));
        submissionUseCase.findForTeacher(teacherEmail, job.getSubmissionId());
        return job;
    }

    @Override
    public List<AiGradingSuggestion> listSuggestions(String teacherEmail, UUID submissionId) {
        requireVipTeacher(teacherEmail);
        submissionUseCase.findForTeacher(teacherEmail, submissionId);
        return suggestionRepository.findBySubmissionId(submissionId);
    }

    @Override
    public AiGradingSuggestion accept(String teacherEmail, UUID suggestionId) {
        requireVipTeacher(teacherEmail);
        AiGradingSuggestion suggestion = requireReviewableSuggestion(teacherEmail, suggestionId);
        submissionUseCase.grade(teacherEmail, suggestion.getSubmissionId(), List.of(
                new ExamSubmissionUseCase.GradeUpdate(suggestion.getQuestionId(),
                        suggestion.getSuggestedScore(), suggestion.getSuggestedComment())));
        User teacher = requireTeacher(teacherEmail);
        for (AiGradingSuggestion previous : suggestionRepository.findByAnswerId(suggestion.getAnswerId())) {
            if (previous.getStatus() == AiGradingSuggestionStatus.ACCEPTED && !previous.getId().equals(suggestionId)) {
                suggestionRepository.save(previous.toBuilder().status(AiGradingSuggestionStatus.SUPERSEDED).build());
            }
        }
        return suggestionRepository.save(suggestion.toBuilder().status(AiGradingSuggestionStatus.ACCEPTED)
                .reviewedAt(Instant.now()).reviewedByTeacherId(teacher.getId()).build());
    }

    @Override
    public AiGradingSuggestion reject(String teacherEmail, UUID suggestionId) {
        requireVipTeacher(teacherEmail);
        AiGradingSuggestion suggestion = requireReviewableSuggestion(teacherEmail, suggestionId);
        User teacher = requireTeacher(teacherEmail);
        return suggestionRepository.save(suggestion.toBuilder().status(AiGradingSuggestionStatus.REJECTED)
                .reviewedAt(Instant.now()).reviewedByTeacherId(teacher.getId()).build());
    }

    private AiGradingSuggestion requireReviewableSuggestion(String teacherEmail, UUID suggestionId) {
        AiGradingSuggestion suggestion = suggestionRepository.findById(suggestionId)
                .orElseThrow(() -> new IllegalArgumentException("Sugerencia de IA no encontrada"));
        requireGradableSubmission(teacherEmail, suggestion.getSubmissionId());
        if (suggestion.getStatus() == AiGradingSuggestionStatus.FAILED) {
            throw new IllegalArgumentException("No se puede revisar una sugerencia fallida");
        }
        if (suggestion.getStatus() != AiGradingSuggestionStatus.READY) {
            throw new IllegalArgumentException("La sugerencia ya fue revisada");
        }
        return suggestion;
    }

    private ExamSubmission requireGradableSubmission(String teacherEmail, UUID submissionId) {
        ExamSubmission submission = submissionUseCase.findForTeacher(teacherEmail, submissionId);
        if (submission.getStatus() != SubmissionStatus.ENTREGADO) {
            throw new IllegalArgumentException("La entrega debe estar finalizada");
        }
        Exam exam = examUseCase.findById(submission.getExamId());
        if (exam.isFeedbackPublished()) {
            throw new IllegalArgumentException("Las devoluciones ya fueron publicadas");
        }
        return submission;
    }

    private User requireTeacher(String teacherEmail) {
        User teacher = userRepository.findByEmail(teacherEmail)
                .orElseThrow(() -> new IllegalArgumentException("Profesor no encontrado"));
        if (teacher.getRole() != Role.PROFESOR) throw new IllegalArgumentException("Solo un profesor puede usar IA");
        return teacher;
    }

    private void requireVipTeacher(String teacherEmail) {
        String configuredVip = properties.getAiGrading().getVipTeacherEmail();
        String normalizedEmail = teacherEmail == null ? "" : teacherEmail.trim().toLowerCase(Locale.ROOT);
        String normalizedVip = configuredVip == null ? "" : configuredVip.trim().toLowerCase(Locale.ROOT);
        if (!normalizedEmail.equals(normalizedVip)) {
            throw new AccessDeniedException("La correccion con IA esta reservada al docente VIP");
        }
    }
}
