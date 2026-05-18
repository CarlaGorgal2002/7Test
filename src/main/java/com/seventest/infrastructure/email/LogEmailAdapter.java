package com.seventest.infrastructure.email;

import com.seventest.domain.port.out.EmailPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class LogEmailAdapter implements EmailPort {

    @Override
    public void sendPasswordRecoveryNotification(String userEmail, String userFullName) {
        log.info("[EMAIL] Recuperación de contraseña solicitada para: {} <{}>", userFullName, userEmail);
    }
}
