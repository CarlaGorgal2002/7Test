package com.seventest.infrastructure.web.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public record ExamRequest(
        @NotBlank(message = "El titulo es obligatorio")
        @Size(max = 160, message = "El titulo no puede superar 160 caracteres")
        String title,

        @Size(max = 2000, message = "La descripcion no puede superar 2000 caracteres")
        String description,

        @Size(max = 120, message = "La materia no puede superar 120 caracteres")
        String courseName,

        Instant availableFrom,

        @Min(value = 1, message = "La duracion debe ser mayor a cero")
        Integer durationMinutes
) {
}
