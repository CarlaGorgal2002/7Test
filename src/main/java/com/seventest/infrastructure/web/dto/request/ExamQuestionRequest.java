package com.seventest.infrastructure.web.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record ExamQuestionRequest(
        @Size(max = 4000, message = "El enunciado no puede superar 4000 caracteres")
        String prompt,

        @Size(max = 20000, message = "La respuesta modelo no puede superar 20000 caracteres")
        String modelAnswer,

        @NotNull(message = "El puntaje es obligatorio")
        @DecimalMin(value = "0.01", message = "El puntaje debe ser mayor a cero")
        @DecimalMax(value = "10.00", message = "El puntaje no puede superar 10")
        BigDecimal points
) {
}
