package com.seventest.infrastructure.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.seventest.domain.model.*;
import com.seventest.domain.port.out.*;
import com.seventest.infrastructure.config.AppProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class AiGradingWorkerTest {
    @Mock private AiGradingJobRepository jobRepository;
    @Mock private AiGradingSuggestionRepository suggestionRepository;
    @Mock private ExamSubmissionRepository submissionRepository;
    @Mock private ExamRepository examRepository;
    @Mock private AiCorrectionProvider provider;

    private AiGradingWorker worker;
    private UUID jobId;
    private UUID submissionId;
    private UUID topicId;
    private UUID teacherId;

    @BeforeEach
    void setUp() {
        AppProperties properties = new AppProperties();
        AppProperties.AiGrading config = new AppProperties.AiGrading();
        config.setModel("fake-model");
        config.setPromptVersion("prompt-v1");
        config.setMaterialVersion("material-v1");
        config.setMaterialSha256("A".repeat(64));
        properties.setAiGrading(config);
        worker = new AiGradingWorker(jobRepository, suggestionRepository, submissionRepository, examRepository,
                provider, properties, new ObjectMapper());
        jobId = UUID.randomUUID();
        submissionId = UUID.randomUUID();
        topicId = UUID.randomUUID();
        teacherId = UUID.randomUUID();
        Mockito.lenient().when(jobRepository.save(Mockito.any())).thenAnswer(invocation -> invocation.getArgument(0));
        Mockito.lenient().when(suggestionRepository.save(Mockito.any())).thenAnswer(invocation -> invocation.getArgument(0));
        Mockito.lenient().when(suggestionRepository.nextAttemptNumber(Mockito.any())).thenReturn(1);
    }

    @Test
    void blankAnswerCreatesZeroSuggestionWithoutCallingProvider() {
        Fixture fixture = fixture(List.of(""), List.of(new BigDecimal("1.25")));
        stub(fixture);

        worker.dispatch(jobId);

        ArgumentCaptor<AiGradingSuggestion> suggestion = ArgumentCaptor.forClass(AiGradingSuggestion.class);
        Mockito.verify(suggestionRepository).save(suggestion.capture());
        assertEquals(0, suggestion.getValue().getSuggestedFraction().compareTo(BigDecimal.ZERO));
        assertEquals(0, suggestion.getValue().getSuggestedScore().compareTo(BigDecimal.ZERO));
        assertEquals(AiGradingSuggestionStatus.READY, suggestion.getValue().getStatus());
        Mockito.verifyNoInteractions(provider);
        assertFinalStatus(AiGradingJobStatus.COMPLETED);
    }

    @Test
    void continuesSequentiallyAfterIndividualProviderFailure() {
        Fixture fixture = fixture(List.of("primera", "segunda"), List.of(BigDecimal.ONE, new BigDecimal("1.25")));
        stub(fixture);
        AiCorrectionProvider.Result result = new AiCorrectionProvider.Result(new BigDecimal("0.25"), "Parcial",
                List.of("Bien"), List.of("Falta"), List.of(12), AiGradingConfidence.MEDIUM, false, "");
        Mockito.when(provider.evaluate(Mockito.any())).thenThrow(new IllegalStateException("network")).thenReturn(result);

        worker.dispatch(jobId);

        ArgumentCaptor<AiGradingSuggestion> suggestions = ArgumentCaptor.forClass(AiGradingSuggestion.class);
        Mockito.verify(suggestionRepository, Mockito.times(2)).save(suggestions.capture());
        assertEquals(AiGradingSuggestionStatus.FAILED, suggestions.getAllValues().get(0).getStatus());
        assertEquals(AiGradingSuggestionStatus.READY, suggestions.getAllValues().get(1).getStatus());
        assertEquals(0, suggestions.getAllValues().get(1).getSuggestedScore().compareTo(new BigDecimal("0.3125")));
        Mockito.verify(provider, Mockito.times(2)).evaluate(Mockito.any());
        assertFinalStatus(AiGradingJobStatus.PARTIAL_FAILURE);
    }

    @Test
    void sendsNormalizedDecisionTableDiagnosticsAndForcesReviewWithoutPages() {
        String table = "7TEST_DECISION_TABLE:{\"rows\":2,\"cols\":2,\"cells\":[[\"Condicion\",\"Si\"],[\"Accion\",\"\"]]}";
        Fixture fixture = fixture(List.of(table), List.of(BigDecimal.ONE));
        stub(fixture);
        Mockito.when(provider.evaluate(Mockito.any())).thenReturn(new AiCorrectionProvider.Result(
                new BigDecimal("0.75"), "Bien", List.of(), List.of(), List.of(),
                AiGradingConfidence.HIGH, false, ""));

        worker.dispatch(jobId);

        ArgumentCaptor<AiCorrectionProvider.Request> request = ArgumentCaptor.forClass(AiCorrectionProvider.Request.class);
        Mockito.verify(provider).evaluate(request.capture());
        assertEquals("DECISION_TABLE", request.getValue().questionType());
        assertTrue(request.getValue().structuralDiagnostics().contains("filas=2"));
        ArgumentCaptor<AiGradingSuggestion> suggestion = ArgumentCaptor.forClass(AiGradingSuggestion.class);
        Mockito.verify(suggestionRepository).save(suggestion.capture());
        assertTrue(suggestion.getValue().isRequiresHumanReview());
    }

    @Test
    void invalidProviderFractionBecomesIndependentFailure() {
        Fixture fixture = fixture(List.of("respuesta"), List.of(BigDecimal.ONE));
        stub(fixture);
        Mockito.when(provider.evaluate(Mockito.any())).thenReturn(new AiCorrectionProvider.Result(
                new BigDecimal("0.33"), "No valida", List.of(), List.of(), List.of(1),
                AiGradingConfidence.HIGH, false, ""));

        worker.dispatch(jobId);

        ArgumentCaptor<AiGradingSuggestion> suggestion = ArgumentCaptor.forClass(AiGradingSuggestion.class);
        Mockito.verify(suggestionRepository).save(suggestion.capture());
        assertEquals(AiGradingSuggestionStatus.FAILED, suggestion.getValue().getStatus());
        assertFinalStatus(AiGradingJobStatus.PARTIAL_FAILURE);
    }

    @Test
    void refusesQueuedJobWhenRequestingTeacherNoLongerOwnsExam() {
        Fixture fixture = fixture(List.of("respuesta"), List.of(BigDecimal.ONE));
        teacherId = UUID.randomUUID();
        stub(fixture);

        worker.dispatch(jobId);

        Mockito.verifyNoInteractions(provider, suggestionRepository);
        assertFinalStatus(AiGradingJobStatus.FAILED);
    }

    private void stub(Fixture fixture) {
        AiGradingJob queued = AiGradingJob.builder().id(jobId).submissionId(submissionId)
                .requestedByTeacherId(teacherId)
                .status(AiGradingJobStatus.QUEUED).totalQuestions(fixture.answers.size()).build();
        Mockito.when(jobRepository.findById(jobId)).thenReturn(Optional.of(queued));
        Mockito.when(submissionRepository.findById(submissionId)).thenReturn(Optional.of(fixture.submission));
        Mockito.when(examRepository.findById(fixture.submission.getExamId())).thenReturn(Optional.of(fixture.exam));
    }

    private Fixture fixture(List<String> texts, List<BigDecimal> points) {
        UUID examId = UUID.randomUUID();
        var questions = new java.util.ArrayList<ExamQuestion>();
        var answers = new java.util.ArrayList<ExamAnswer>();
        for (int i = 0; i < texts.size(); i++) {
            UUID questionId = UUID.randomUUID();
            questions.add(ExamQuestion.builder().id(questionId).prompt("Pregunta").modelAnswer("Modelo")
                    .teacherCriteria("Criterio").points(points.get(i)).displayOrder(i + 1).build());
            answers.add(ExamAnswer.builder().id(UUID.randomUUID()).questionId(questionId).answerText(texts.get(i)).build());
        }
        ExamSubmission submission = ExamSubmission.builder().id(submissionId).examId(examId).topicId(topicId)
                .status(SubmissionStatus.ENTREGADO).answers(answers).build();
        Exam exam = Exam.builder().id(examId).teacherId(teacherId).feedbackPublished(false)
                .topics(List.of(ExamTopic.builder().id(topicId).questions(questions).build())).build();
        return new Fixture(submission, exam, answers);
    }

    private void assertFinalStatus(AiGradingJobStatus expected) {
        ArgumentCaptor<AiGradingJob> jobs = ArgumentCaptor.forClass(AiGradingJob.class);
        Mockito.verify(jobRepository, Mockito.atLeast(2)).save(jobs.capture());
        assertEquals(expected, jobs.getAllValues().get(jobs.getAllValues().size() - 1).getStatus());
    }

    private record Fixture(ExamSubmission submission, Exam exam, List<ExamAnswer> answers) {}
}
