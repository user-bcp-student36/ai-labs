package com.bcp.workshop.auth.security;

import jakarta.enterprise.context.ApplicationScoped;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;

@ApplicationScoped
public class TokenIssuer {
    public static final int TTL_SECONDS = 3600;
    // Formato compartido con api/app/main.py; cambiarlo requiere cambio coordinado con la API.
    private static final String SHARED_SECRET = "WORKSHOP_SECRET_2026";
    private static final String SEPARATOR = "|";

    public String issue(String username, String role) {
        long expiry = Instant.now().plusSeconds(TTL_SECONDS).getEpochSecond();
        String value = String.join(SEPARATOR, username, role, String.valueOf(expiry), SHARED_SECRET);
        return Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }
}
