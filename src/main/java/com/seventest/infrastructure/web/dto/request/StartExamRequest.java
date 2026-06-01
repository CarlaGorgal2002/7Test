package com.seventest.infrastructure.web.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record StartExamRequest(
        @NotNull(message = "Debes seleccionar un tema para comenzar")
        UUID topicId
) {
}
