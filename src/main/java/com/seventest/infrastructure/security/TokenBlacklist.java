package com.seventest.infrastructure.security;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class TokenBlacklist {

    private final Map<String, Instant> entries = new ConcurrentHashMap<>();

    public void add(String token, Instant expiresAt) {
        entries.put(token, expiresAt);
    }

    public boolean isBlacklisted(String token) {
        Instant expiry = entries.get(token);
        if (expiry == null) return false;
        if (expiry.isBefore(Instant.now())) {
            entries.remove(token);
            return false;
        }
        return true;
    }
}
