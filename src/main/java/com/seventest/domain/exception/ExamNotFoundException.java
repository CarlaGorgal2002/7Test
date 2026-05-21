package com.seventest.domain.exception;

import java.util.UUID;

public class ExamNotFoundException extends RuntimeException {
    public ExamNotFoundException(UUID id) {
        super("Examen no encontrado: " + id);
    }
}
