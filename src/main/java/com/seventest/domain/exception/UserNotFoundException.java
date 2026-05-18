package com.seventest.domain.exception;

import java.util.UUID;

public class UserNotFoundException extends RuntimeException {
    public UserNotFoundException(UUID id) {
        super("Usuario no encontrado: " + id);
    }
}
