package com.seventest.domain.port.in;

import com.seventest.domain.model.LoginResult;

public interface AuthUseCase {
    LoginResult login(String email, String password);
    void logout(String token);
    void requestPasswordRecovery(String email);
    String recoverByName(String fullName);
}
