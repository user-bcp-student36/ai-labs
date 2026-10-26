package com.bcp.workshop.auth.repository;

import com.bcp.workshop.auth.security.PasswordHasher;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserStoreInitializerTest {
    private final PasswordHasher hasher = new PasswordHasher();

    @Test
    void seedsStudentAndTrainerWithHashedPasswords() {
        RecordingUsers users = new RecordingUsers();
        UserStoreInitializer initializer = new UserStoreInitializer(users, hasher, "estudiante-prueba", "entrenador-prueba");

        initializer.onStart(null);

        assertEquals(1, users.schemaCreations);
        assertEquals(2, users.rows.size());
        assertTrue(hasher.matches("estudiante-prueba", users.rows.get("student")));
        assertTrue(hasher.matches("entrenador-prueba", users.rows.get("trainer")));
    }

    private static final class RecordingUsers implements UserRepository {
        private final Map<String, String> rows = new HashMap<>();
        private int schemaCreations;

        @Override
        public void createSchema() {
            schemaCreations++;
        }

        @Override
        public void addIfAbsent(String username, String passwordHash, String displayName, String role) {
            rows.putIfAbsent(username, passwordHash);
        }

        @Override
        public Optional<UserAccount> findByUsername(String username) {
            return Optional.empty();
        }
    }
}
