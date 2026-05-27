package com.seventest.infrastructure.web.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public record SaveAnswersRequest(
        @Valid
        @NotNull(message = "La lista de respuestas es obligatoria")
        List<AnswerRequest> answers
) {
    public record AnswerRequest(
            @NotNull(message = "El id de pregunta es obligatorio")
            UUID questionId,

            @Size(max = 20000, message = "La respuesta no puede superar 20000 caracteres")
            String answerText
    ) {
    }
}
