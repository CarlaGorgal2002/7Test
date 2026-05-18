package com.seventest.domain.port.out;

public interface EmailPort {
    void sendPasswordRecoveryNotification(String userEmail, String userFullName);
}
