package com.bcp.workshop.auth.security;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;

@ApplicationScoped
public class TokenIssuer {
    public static final int TTL_SECONDS = 3600;
    // Formato compartido con api/app/main.py; cambiarlo requiere cambio coordinado con la API.
    private static final String SEPARATOR = "|";

    private final String sharedSecret;

    @Inject
    public TokenIssuer(@ConfigProperty(name = "auth.token.secret") String sharedSecret) {
        this.sharedSecret = sharedSecret;
    }

    public String issue(String username, String role) {
        String value = String.join(SEPARATOR, username, role, expiryEpoch(), sharedSecret);
        return Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private String expiryEpoch() {
        return String.valueOf(Instant.now().plusSeconds(TTL_SECONDS).getEpochSecond());
    }
}
