package com.seventest.application.service;

import com.seventest.domain.model.*;
import com.seventest.domain.port.in.ExamManagementUseCase;
import com.seventest.domain.port.in.ExamSubmissionUseCase;
import com.seventest.domain.port.out.*;
import com.seventest.infrastructure.config.AppProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class AiCorrectionServiceTest {
    private static final String EMAIL = "teacher@test.com";
    private final UUID submissionId = UUID.randomUUID();
    private final UUID examId = UUID.randomUUID();
    private final UUID teacherId = UUID.randomUUID();

    @Mock private ExamSubmissionUseCase submissionUseCase;
    @Mock private ExamManagementUseCase examUseCase;
    @Mock private UserRepository userRepository;
    @Mock private AiGradingJobRepository jobRepository;
    @Mock private AiGradingSuggestionRepository suggestionRepository;
    @Mock private AiGradingJobDispatcher dispatcher;
    @Mock private AiCorrectionProvider provider;

    private AppProperties properties;
    private AiCorrectionService service;

    @BeforeEach
    void setUp() {
        properties = new AppProperties();
        AppProperties.AiGrading config = new AppProperties.AiGrading();
        config.setEnabled(true);
        config.setApiKey("secret");
        config.setModel("model");
        config.setMaterialVersion("material");
        config.setPromptVersion("prompt");
        properties.setAiGrading(config);
        service = new AiCorrectionService(properties, submissionUseCase, examUseCase, userRepository,
                jobRepository, suggestionRepository, dispatcher, provider);
    }

    @Test
    void statusReportsSafeConfiguration() {
        AiGradingStatus status = service.status();
        assertTrue(status.enabled());
        assertTrue(status.available());
        assertEquals("model", status.model());
        assertEquals("material", status.materialVersion());
        assertEquals("prompt", status.promptVersion());
        assertEquals("OpenAI esta configurado; falta comprobar conectividad.", status.message());
    }

    @Test
    void checkStatusReportsRealProviderAvailability() {
        Mockito.when(provider.checkAvailability())
                .thenReturn(new AiCorrectionProvider.Availability(false, "Proveedor inaccesible"));

        AiGradingStatus status = service.checkStatus();

        assertFalse(status.available());
        assertEquals("Proveedor inaccesible", status.message());
    }

    @Test
    void statusAndCheckReportDisabledConfigurationWithoutCallingProvider() {
        properties.getAiGrading().setEnabled(false);

        AiGradingStatus status = service.status();
        AiGradingStatus checked = service.checkStatus();

        assertFalse(status.available());
        assertEquals("OpenAI no esta configurado. La correccion manual sigue disponible.", status.message());
        assertEquals(status, checked);
        Mockito.verifyNoInteractions(provider);
    }

    @Test
    void startRejectsWhenDisabled() {
        properties.getAiGrading().setEnabled(false);
        assertThrows(IllegalArgumentException.class, () -> service.startJob(EMAIL, submissionId));
    }

    @Test
    void startReturnsExistingActiveJob() {
        stubGradable();
        AiGradingJob active = job(AiGradingJobStatus.RUNNING);
        Mockito.when(jobRepository.findActiveBySubmissionId(submissionId)).thenReturn(Optional.of(active));
        assertSame(active, service.startJob(EMAIL, submissionId));
        Mockito.verifyNoInteractions(dispatcher);
    }

    @Test
    void startCreatesAndDispatchesJob() {
        stubGradable();
        Mockito.when(jobRepository.findActiveBySubmissionId(submissionId)).thenReturn(Optional.empty());
        Mockito.when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(teacher(Role.PROFESOR)));
        Mockito.when(jobRepository.save(Mockito.any())).thenAnswer(invocation -> invocation.getArgument(0));

        AiGradingJob job = service.startJob(EMAIL, submissionId);

        assertEquals(AiGradingJobStatus.QUEUED, job.getStatus());
        assertEquals(1, job.getTotalQuestions());
        Mockito.verify(dispatcher).dispatch(job.getId());
    }

    @Test
    void findJobRejectsMissingJob() {
        Mockito.when(jobRepository.findById(Mockito.any())).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> service.findJob(EMAIL, UUID.randomUUID()));
    }

    @Test
    void findJobAuthorizesThroughSubmission() {
        AiGradingJob job = job(AiGradingJobStatus.COMPLETED);
        Mockito.when(jobRepository.findById(job.getId())).thenReturn(Optional.of(job));
        assertSame(job, service.findJob(EMAIL, job.getId()));
        Mockito.verify(submissionUseCase).findForTeacher(EMAIL, submissionId);
    }

    @Test
    void listSuggestionsAuthorizesAndReturnsHistory() {
        AiGradingSuggestion suggestion = suggestion(AiGradingSuggestionStatus.READY);
        Mockito.when(suggestionRepository.findBySubmissionId(submissionId)).thenReturn(List.of(suggestion));
        assertEquals(List.of(suggestion), service.listSuggestions(EMAIL, submissionId));
        Mockito.verify(submissionUseCase).findForTeacher(EMAIL, submissionId);
    }

    @Test
    void acceptCopiesGradeAndSupersedesOnlyPreviousAcceptedSuggestion() {
        stubGradable();
        AiGradingSuggestion current = suggestion(AiGradingSuggestionStatus.READY);
        AiGradingSuggestion previous = suggestion(AiGradingSuggestionStatus.ACCEPTED).toBuilder().id(UUID.randomUUID()).build();
        AiGradingSuggestion sameIdAccepted = previous.toBuilder().id(current.getId()).build();
        AiGradingSuggestion rejected = suggestion(AiGradingSuggestionStatus.REJECTED).toBuilder().id(UUID.randomUUID()).build();
        Mockito.when(suggestionRepository.findById(current.getId())).thenReturn(Optional.of(current));
        Mockito.when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(teacher(Role.PROFESOR)));
        Mockito.when(suggestionRepository.findByAnswerId(current.getAnswerId()))
                .thenReturn(List.of(previous, sameIdAccepted, rejected));
        Mockito.when(suggestionRepository.save(Mockito.any())).thenAnswer(invocation -> invocation.getArgument(0));

        AiGradingSuggestion accepted = service.accept(EMAIL, current.getId());

        assertEquals(AiGradingSuggestionStatus.ACCEPTED, accepted.getStatus());
        Mockito.verify(submissionUseCase).grade(EMAIL, submissionId, List.of(
                new ExamSubmissionUseCase.GradeUpdate(current.getQuestionId(), current.getSuggestedScore(), current.getSuggestedComment())));
        Mockito.verify(suggestionRepository, Mockito.times(2)).save(Mockito.any());
    }

    @Test
    void rejectMarksSuggestionReviewed() {
        stubGradable();
        AiGradingSuggestion current = suggestion(AiGradingSuggestionStatus.READY);
        Mockito.when(suggestionRepository.findById(current.getId())).thenReturn(Optional.of(current));
        Mockito.when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(teacher(Role.PROFESOR)));
        Mockito.when(suggestionRepository.save(Mockito.any())).thenAnswer(invocation -> invocation.getArgument(0));
        assertEquals(AiGradingSuggestionStatus.REJECTED, service.reject(EMAIL, current.getId()).getStatus());
    }

    @Test
    void reviewRejectsMissingFailedAndAlreadyReviewedSuggestions() {
        UUID id = UUID.randomUUID();
        Mockito.when(suggestionRepository.findById(id)).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> service.reject(EMAIL, id));

        AiGradingSuggestion failed = suggestion(AiGradingSuggestionStatus.FAILED);
        Mockito.when(suggestionRepository.findById(failed.getId())).thenReturn(Optional.of(failed));
        stubGradable();
        assertThrows(IllegalArgumentException.class, () -> service.reject(EMAIL, failed.getId()));

        AiGradingSuggestion accepted = suggestion(AiGradingSuggestionStatus.ACCEPTED);
        Mockito.when(suggestionRepository.findById(accepted.getId())).thenReturn(Optional.of(accepted));
        assertThrows(IllegalArgumentException.class, () -> service.reject(EMAIL, accepted.getId()));
    }

    @Test
    void startRejectsUnsubmittedAndPublishedFeedback() {
        ExamSubmission inProgress = submission(SubmissionStatus.EN_PROGRESO);
        Mockito.when(submissionUseCase.findForTeacher(EMAIL, submissionId)).thenReturn(inProgress);
        assertThrows(IllegalArgumentException.class, () -> service.startJob(EMAIL, submissionId));

        Mockito.when(submissionUseCase.findForTeacher(EMAIL, submissionId)).thenReturn(submission(SubmissionStatus.ENTREGADO));
        Mockito.when(examUseCase.findById(examId)).thenReturn(exam(true));
        assertThrows(IllegalArgumentException.class, () -> service.startJob(EMAIL, submissionId));
    }

    @Test
    void startRejectsMissingAndNonTeacherUsers() {
        stubGradable();
        Mockito.when(jobRepository.findActiveBySubmissionId(submissionId)).thenReturn(Optional.empty());
        Mockito.when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> service.startJob(EMAIL, submissionId));

        Mockito.when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(teacher(Role.ALUMNO)));
        assertThrows(IllegalArgumentException.class, () -> service.startJob(EMAIL, submissionId));
    }

    private void stubGradable() {
        Mockito.lenient().when(submissionUseCase.findForTeacher(EMAIL, submissionId))
                .thenReturn(submission(SubmissionStatus.ENTREGADO));
        Mockito.lenient().when(examUseCase.findById(examId)).thenReturn(exam(false));
    }

    private ExamSubmission submission(SubmissionStatus status) {
        return ExamSubmission.builder().id(submissionId).examId(examId).status(status)
                .answers(List.of(ExamAnswer.builder().id(UUID.randomUUID()).questionId(UUID.randomUUID()).answerText("answer").build()))
                .build();
    }

    private Exam exam(boolean feedbackPublished) {
        return Exam.builder().id(examId).teacherId(teacherId).feedbackPublished(feedbackPublished).build();
    }

    private User teacher(Role role) {
        return User.builder().id(teacherId).email(EMAIL).role(role).build();
    }

    private AiGradingJob job(AiGradingJobStatus status) {
        return AiGradingJob.builder().id(UUID.randomUUID()).submissionId(submissionId).status(status).build();
    }

    private AiGradingSuggestion suggestion(AiGradingSuggestionStatus status) {
        return AiGradingSuggestion.builder().id(UUID.randomUUID()).submissionId(submissionId).answerId(UUID.randomUUID())
                .questionId(UUID.randomUUID()).status(status).suggestedScore(new BigDecimal("0.3125"))
                .suggestedComment("Comentario").build();
    }
}
