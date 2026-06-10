package com.seventest.domain.exception;

import lombok.Getter;

@Getter
public class AiCorrectionProviderException extends RuntimeException {
    private final Reason reason;
    private final String safeMessage;

    public AiCorrectionProviderException(Reason reason, String safeMessage, Throwable cause) {
        super(safeMessage, cause);
        this.reason = reason;
        this.safeMessage = safeMessage;
    }

    public enum Reason {
        AUTHENTICATION,
        QUOTA,
        TIMEOUT,
        MATERIAL,
        MODEL,
        LOCATION,
        SAFETY,
        INVALID_REQUEST,
        INVALID_RESPONSE,
        UNAVAILABLE
    }
}
