package com.bcp.workshop.auth.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordHasherTest {
    private static final String PASSWORD = "clave-de-prueba-1";
    private final PasswordHasher hasher = new PasswordHasher();

    @Test
    void matchesOnlyTheOriginalPassword() {
        String stored = hasher.hash(PASSWORD);

        assertTrue(hasher.matches(PASSWORD, stored));
        assertFalse(hasher.matches("otra-clave", stored));
    }

    @Test
    void usesRandomSaltForEachHash() {
        assertNotEquals(hasher.hash(PASSWORD), hasher.hash(PASSWORD));
    }

    @Test
    void doesNotStorePlainPassword() {
        assertFalse(hasher.hash(PASSWORD).contains(PASSWORD));
    }

    @Test
    void rejectsMalformedStoredValues() {
        assertFalse(hasher.matches(PASSWORD, "Workshop123!"));
        assertFalse(hasher.matches(PASSWORD, "pbkdf2_sha256$abc$!!$!!"));
        assertFalse(hasher.matches(PASSWORD, "otro_esquema$1$AAAA$AAAA"));
    }
}
