package com.seventest.domain.exception;

public class PasswordPolicyViolationException extends RuntimeException {
    public PasswordPolicyViolationException(String message) {
        super(message);
    }
}
