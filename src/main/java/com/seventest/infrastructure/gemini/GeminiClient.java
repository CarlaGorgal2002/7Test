package com.seventest.infrastructure.gemini;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class GeminiClient {

    private final RestClient restClient;
    private final String apiKey;
    private final String model;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public GeminiClient(
            @Value("${app.gemini.api-key:}") String apiKey,
            @Value("${app.gemini.model:gemini-1.5-flash}") String model) {
        this.apiKey = apiKey;
        this.model = model;
        this.restClient = RestClient.builder()
                .baseUrl("https://generativelanguage.googleapis.com")
                .build();
    }

    @Getter
    @Builder
    public static class GradingResult {
        private final BigDecimal accuracy;
        private final String feedback;
    }

    public GradingResult evaluate(String systemInstruction, String promptText) {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("Gemini API key is not configured. Evaluation skipped.");
            throw new IllegalStateException("Gemini API key is not configured.");
        }

        try {
            // Construir el body del request según la especificación de la API de Gemini
            Map<String, Object> requestBody = Map.of(
                    "systemInstruction", Map.of(
                            "parts", List.of(Map.of("text", systemInstruction))
                    ),
                    "contents", List.of(
                            Map.of("parts", List.of(Map.of("text", promptText)))
                    ),
                    "generationConfig", Map.of(
                            "responseMimeType", "application/json",
                            "responseSchema", Map.of(
                                    "type", "OBJECT",
                                    "properties", Map.of(
                                            "accuracy", Map.of(
                                                    "type", "NUMBER",
                                                    "description", "Nivel de exactitud de la respuesta. Debe ser estrictamente un valor de la escala: 0.0, 0.25, 0.5, 0.75, 1.0"
                                            ),
                                            "feedback", Map.of(
                                                    "type", "STRING",
                                                    "description", "Comentario conciso en español que justifique la nota si el accuracy es menor a 1.0. Si es 1.0, puede ser vacío."
                                            )
                                    ),
                                    "required", List.of("accuracy", "feedback")
                            )
                    )
            );

            String url = String.format("/v1beta/models/%s:generateContent?key=%s", model, apiKey);

            String responseJson = restClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .body(String.class);

            // Parsear la respuesta para extraer el JSON generado por el modelo
            // La estructura típica de Gemini es: candidates[0].content.parts[0].text
            var root = objectMapper.readTree(responseJson);
            var text = root.path("candidates")
                    .path(0)
                    .path("content")
                    .path("parts")
                    .path(0)
                    .path("text")
                    .asText();

            if (text == null || text.isBlank()) {
                throw new IllegalStateException("Empty response from Gemini API");
            }

            // Parsear el JSON interno de la corrección
            var gradingNode = objectMapper.readTree(text.trim());
            BigDecimal accuracy = new BigDecimal(gradingNode.path("accuracy").asText("0.0"));
            String feedback = gradingNode.path("feedback").asText("");

            return GradingResult.builder()
                    .accuracy(accuracy)
                    .feedback(feedback)
                    .build();

        } catch (Exception e) {
            log.error("Error invoking Gemini API", e);
            throw new RuntimeException("Error communicating with Gemini API", e);
        }
    }
}
