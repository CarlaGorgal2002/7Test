package com.seventest.infrastructure.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.seventest.domain.exception.AiCorrectionProviderException;
import com.seventest.domain.model.AiGradingConfidence;
import com.seventest.domain.port.out.AiCorrectionProvider;
import com.seventest.infrastructure.config.AppProperties;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;

import java.math.BigDecimal;
import java.net.http.HttpTimeoutException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OpenAiCorrectionAdapterTest {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final AppProperties properties = properties();
    private final OpenAiCorrectionAdapter adapter = new OpenAiCorrectionAdapter(
            null, properties, objectMapper, new DefaultResourceLoader());

    @Test
    void buildsStatelessStructuredResponsesRequest() throws Exception {
        AiCorrectionProvider.Request request = new AiCorrectionProvider.Request(
                "TEXT", "Pregunta", "Modelo", "Criterio", "Respuesta", "Diagnostico");
        CourseMaterialManager.Selection material = new CourseMaterialManager.Selection(List.of(
                new CourseMaterialManager.PageExcerpt(12, "Fragmento relevante")));

        var body = adapter.requestBody(request, material);

        assertEquals("gpt-5.4-mini", body.get("model"));
        assertEquals(false, body.get("store"));
        assertTrue(objectMapper.writeValueAsString(body.get("text")).contains("\"strict\":true"));
        assertFalse(objectMapper.writeValueAsString(body).contains("OPENAI_API_KEY"));
    }

    @Test
    void extractsCompletedOutputText() throws Exception {
        String response = """
                {"status":"completed","output":[{"type":"message","content":[
                {"type":"output_text","text":"{\\"suggestedFraction\\":1}"}
                ]}]}
                """;

        assertEquals("{\"suggestedFraction\":1}", adapter.extractOutputText(objectMapper.readTree(response)));
    }

    @Test
    void rejectsIncompleteResponse() throws Exception {
        assertThrows(IllegalStateException.class,
                () -> adapter.extractOutputText(objectMapper.readTree("{\"status\":\"incomplete\",\"output\":[]}")));
    }

    @Test
    void classifiesTimeoutWithoutExposingDetails() {
        AiCorrectionProviderException exception = adapter.classified(new HttpTimeoutException("sensitive"));

        assertEquals(AiCorrectionProviderException.Reason.TIMEOUT, exception.getReason());
        assertEquals("OpenAI excedio el tiempo disponible para evaluar la respuesta.", exception.getSafeMessage());
        assertFalse(exception.getSafeMessage().contains("sensitive"));
    }

    @Test
    void removesCitationsOutsideProvidedPagesAndForcesHumanReview() {
        AiCorrectionProvider.Result result = new AiCorrectionProvider.Result(
                new BigDecimal("0.75"), "Bien", List.of(), List.of(), List.of(12, 99),
                AiGradingConfidence.HIGH, false, "");
        CourseMaterialManager.Selection material = new CourseMaterialManager.Selection(List.of(
                new CourseMaterialManager.PageExcerpt(12, "fragmento oficial")));

        AiCorrectionProvider.Result restricted = adapter.restrictSources(result, material);

        assertEquals(List.of(12), restricted.sourcePages());
        assertTrue(restricted.requiresHumanReview());
        assertTrue(restricted.reviewReason().contains("no fueron proporcionadas"));
    }

    @Test
    void forcesHumanReviewWhenLocalRetrievalFindsNoMaterial() {
        AiCorrectionProvider.Result result = new AiCorrectionProvider.Result(
                BigDecimal.ONE, "Correcta", List.of(), List.of(), List.of(),
                AiGradingConfidence.HIGH, false, "");

        AiCorrectionProvider.Result restricted = adapter.restrictSources(
                result, new CourseMaterialManager.Selection(List.of()));

        assertTrue(restricted.requiresHumanReview());
        assertTrue(restricted.reviewReason().contains("No se encontraron fragmentos relevantes"));
    }

    @Test
    void academicInputContainsOnlySelectedMaterialExcerpts() throws Exception {
        AiCorrectionProvider.Request request = new AiCorrectionProvider.Request(
                "TEXT", "Pregunta", "Modelo", "Criterio", "Respuesta", "Diagnostico");
        CourseMaterialManager.Selection material = new CourseMaterialManager.Selection(List.of(
                new CourseMaterialManager.PageExcerpt(12, "Fragmento relevante")));

        String input = adapter.academicInput(request, material);

        assertTrue(input.contains("\"allowedSourcePages\":[12]"));
        assertTrue(input.contains("\"officialMaterialExcerpts\":[{\"pageNumber\":12,\"text\":\"Fragmento relevante\"}]"));
        assertTrue(input.contains("\"untrustedStudentAnswer\":\"Respuesta\""));
    }

    private AppProperties properties() {
        AppProperties value = new AppProperties();
        value.getAiGrading().setModel("gpt-5.4-mini");
        value.getAiGrading().setPromptResource("classpath:ai-grading/testing-grading-v2.txt");
        return value;
    }
}
