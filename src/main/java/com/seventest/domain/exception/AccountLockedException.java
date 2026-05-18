package com.seventest.domain.exception;

public class AccountLockedException extends RuntimeException {
    public AccountLockedException() {
        super("La cuenta está bloqueada temporalmente por exceso de intentos fallidos");
    }
}
