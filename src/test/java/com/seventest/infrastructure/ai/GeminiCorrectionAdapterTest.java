package com.seventest.infrastructure.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.genai.errors.ClientException;
import com.seventest.domain.exception.AiCorrectionProviderException;
import com.seventest.domain.model.AiGradingConfidence;
import com.seventest.domain.port.out.AiCorrectionProvider;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GeminiCorrectionAdapterTest {
    private final GeminiCorrectionAdapter adapter = new GeminiCorrectionAdapter(null, null, null, null);

    @Test
    void redactsSensitiveValuesFromInvalidRequestDiagnostic() {
        ClientException failure = new ClientException(400, "Bad Request",
                "Invalid file files/secret-id at https://example.test using AIzaSecretValue");

        String diagnostic = adapter.safeApiDiagnostic(failure);

        assertEquals("Detalle: Invalid file files/[ID] at [URL] using [API_KEY]", diagnostic);
    }

    @Test
    void classifiesUnsupportedProviderLocationWithActionableMessage() {
        ClientException failure = new ClientException(400, "FAILED_PRECONDITION",
                "User location is not supported for the API use.");

        AiCorrectionProviderException exception = adapter.classified(failure);

        assertEquals(AiCorrectionProviderException.Reason.LOCATION, exception.getReason());
        assertEquals("Google rechazo el uso de Gemini Free Tier desde la ubicacion detectada para Render. "
                + "Habilita billing en el proyecto de Google AI Studio; como alternativa, usa otra region/IP "
                + "de salida para el backend.",
                exception.getSafeMessage());
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
        GeminiCorrectionAdapter serializingAdapter = new GeminiCorrectionAdapter(
                null, null, new ObjectMapper(), null);
        AiCorrectionProvider.Request request = new AiCorrectionProvider.Request(
                "TEXT", "Pregunta", "Modelo", "Criterio", "Respuesta", "Diagnostico");
        CourseMaterialManager.Selection material = new CourseMaterialManager.Selection(List.of(
                new CourseMaterialManager.PageExcerpt(12, "Fragmento relevante")));

        String input = serializingAdapter.academicInput(request, material);

        assertTrue(input.contains("\"allowedSourcePages\":[12]"));
        assertTrue(input.contains("\"officialMaterialExcerpts\":[{\"pageNumber\":12,\"text\":\"Fragmento relevante\"}]"));
        assertTrue(input.contains("\"untrustedStudentAnswer\":\"Respuesta\""));
    }
}
