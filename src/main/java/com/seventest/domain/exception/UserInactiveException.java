package com.seventest.domain.exception;

public class UserInactiveException extends RuntimeException {
    public UserInactiveException() {
        super("La cuenta se encuentra inactiva");
    }
}
