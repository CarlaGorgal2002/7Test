package com.seventest.domain.exception;

public class EmailAlreadyExistsException extends RuntimeException {
    public EmailAlreadyExistsException(String email) {
        super("El email ya está registrado en la plataforma: " + email);
    }
}
