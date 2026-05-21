package com.seventest.infrastructure.web.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ExamTopicRequest(
        @NotBlank(message = "El nombre del tema es obligatorio")
        @Size(max = 80, message = "El nombre del tema no puede superar 80 caracteres")
        String name
) {
}
