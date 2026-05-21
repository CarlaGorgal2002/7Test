package com.seventest.domain.exception;

import java.util.UUID;

public class ExamSubmissionNotFoundException extends RuntimeException {
    public ExamSubmissionNotFoundException(UUID id) {
        super("Entrega de examen no encontrada: " + id);
    }
}
