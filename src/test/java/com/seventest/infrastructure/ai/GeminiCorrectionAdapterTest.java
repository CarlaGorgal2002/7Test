package com.seventest.infrastructure.ai;

import com.google.genai.types.File;
import com.google.genai.errors.ClientException;
import com.seventest.domain.exception.AiCorrectionProviderException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GeminiCorrectionAdapterTest {
    private final GeminiCorrectionAdapter adapter = new GeminiCorrectionAdapter(null, null, null, null);

    @Test
    void usesUploadedFileUriForInference() {
        File file = File.builder().name("files/material")
                .uri("https://generativelanguage.googleapis.com/v1beta/files/material").build();

        assertEquals("https://generativelanguage.googleapis.com/v1beta/files/material", adapter.fileUri(file));
    }

    @Test
    void rejectsUploadedFileWithoutInferenceUri() {
        File file = File.builder().name("files/material").build();

        assertThrows(IllegalStateException.class, () -> adapter.fileUri(file));
    }

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
        assertEquals("Google rechazo la IP de salida de Render por ubicacion, aunque el backend esta en "
                + "una region admitida. Reintenta o usa otra region/IP de salida de Render.",
                exception.getSafeMessage());
    }

}
