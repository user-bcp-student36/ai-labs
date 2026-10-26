package com.bcp.workshop.auth.security;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TokenIssuerTest {
    private static final String DEFAULT_SECRET = "WORKSHOP_SECRET_2026";
    private static final String CONFIGURED_SECRET = "secreto-de-prueba-aislado";

    @Test
    void issuesTokenWithUsernameRoleAndExpiry() {
        String token = new TokenIssuer(DEFAULT_SECRET).issue("trainer", "TRAINER");

        String[] parts = new String(Base64.getDecoder().decode(token), StandardCharsets.UTF_8).split("\\|");

        assertEquals(4, parts.length);
        assertEquals("trainer", parts[0]);
        assertEquals("TRAINER", parts[1]);
        long now = Instant.now().getEpochSecond();
        long expiry = Long.parseLong(parts[2]);
        assertTrue(expiry > now + TokenIssuer.TTL_SECONDS - 60 && expiry <= now + TokenIssuer.TTL_SECONDS);
        assertEquals(DEFAULT_SECRET, parts[3]);
    }

    @Test
    void usesConfiguredSecretInsteadOfDefault() {
        String token = new TokenIssuer(CONFIGURED_SECRET).issue("trainer", "TRAINER");

        String[] parts = new String(Base64.getDecoder().decode(token), StandardCharsets.UTF_8).split("\\|");

        assertEquals(CONFIGURED_SECRET, parts[3]);
    }
}
