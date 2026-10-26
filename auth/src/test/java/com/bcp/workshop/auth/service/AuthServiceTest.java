package com.bcp.workshop.auth.service;

import com.bcp.workshop.auth.repository.UserAccount;
import com.bcp.workshop.auth.repository.UserRepository;
import com.bcp.workshop.auth.security.PasswordHasher;
import com.bcp.workshop.auth.security.TokenIssuer;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static jakarta.ws.rs.core.Response.Status.BAD_REQUEST;
import static jakarta.ws.rs.core.Response.Status.UNAUTHORIZED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AuthServiceTest {
    private static final String PASSWORD = "clave-de-prueba-1";
    private static final String TEST_SECRET = "secreto-de-prueba-aislado";
    private final PasswordHasher hasher = new PasswordHasher();
    private final Map<String, UserAccount> store = new HashMap<>();
    private final UserRepository users = new InMemoryUsers(store);
    private final AuthService service = new AuthService(users, hasher, new TokenIssuer(TEST_SECRET));

    AuthServiceTest() {
        store.put("ana", new UserAccount(1L, "ana", hasher.hash(PASSWORD), "Ana Prueba", "STUDENT"));
    }

    @Test
    void returnsSessionForValidCredentials() {
        Session session = service.login("ana", PASSWORD);

        assertNotNull(session.token());
        assertEquals(TokenIssuer.TTL_SECONDS, session.expiresIn());
        assertEquals(new AuthenticatedUser("ana", "Ana Prueba", "STUDENT"), session.user());
    }

    @Test
    void rejectsBlankUsername() {
        assertRejected(() -> service.login("  ", PASSWORD), BAD_REQUEST, "Username is required");
    }

    @Test
    void rejectsMissingPassword() {
        assertRejected(() -> service.login("ana", null), BAD_REQUEST, "Password is required");
    }

    @Test
    void rejectsUnknownUserAndWrongPasswordWithSameMessage() {
        assertRejected(() -> service.login("nadie", PASSWORD), UNAUTHORIZED, "Invalid username or password");
        assertRejected(() -> service.login("ana", "otra"), UNAUTHORIZED, "Invalid username or password");
    }

    @Test
    void propagatesRepositoryFailure() {
        AuthService failing = new AuthService(new FailingUsers(), hasher, new TokenIssuer(TEST_SECRET));

        assertThrows(IllegalStateException.class, () -> failing.login("ana", PASSWORD));
    }

    private static void assertRejected(Runnable action, jakarta.ws.rs.core.Response.Status status, String message) {
        AuthException error = assertThrows(AuthException.class, action::run);
        assertEquals(status, error.status());
        assertEquals(message, error.getMessage());
    }

    private record InMemoryUsers(Map<String, UserAccount> store) implements UserRepository {
        @Override
        public void createSchema() {}

        @Override
        public void addIfAbsent(String username, String passwordHash, String displayName, String role) {}

        @Override
        public Optional<UserAccount> findByUsername(String username) {
            return Optional.ofNullable(store.get(username));
        }
    }

    private static final class FailingUsers implements UserRepository {
        @Override
        public void createSchema() {}

        @Override
        public void addIfAbsent(String username, String passwordHash, String displayName, String role) {}

        @Override
        public Optional<UserAccount> findByUsername(String username) {
            throw new IllegalStateException("database unavailable");
        }
    }
}
