package com.bcp.workshop.auth.service;

import com.bcp.workshop.auth.repository.UserAccount;
import com.bcp.workshop.auth.repository.UserRepository;
import com.bcp.workshop.auth.security.PasswordHasher;
import com.bcp.workshop.auth.security.TokenIssuer;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

import static jakarta.ws.rs.core.Response.Status.BAD_REQUEST;
import static jakarta.ws.rs.core.Response.Status.UNAUTHORIZED;

@ApplicationScoped
public class AuthService {
    private static final Logger LOG = Logger.getLogger(AuthService.class);
    private static final String INVALID_CREDENTIALS = "Invalid username or password";

    private final UserRepository users;
    private final PasswordHasher passwordHasher;
    private final TokenIssuer tokenIssuer;

    public AuthService(UserRepository users, PasswordHasher passwordHasher, TokenIssuer tokenIssuer) {
        this.users = users;
        this.passwordHasher = passwordHasher;
        this.tokenIssuer = tokenIssuer;
    }

    public Session login(String username, String password) {
        requireText(username, "Username is required");
        requireText(password, "Password is required");
        UserAccount account = users.findByUsername(username)
                .filter(candidate -> passwordHasher.matches(password, candidate.passwordHash()))
                .orElseThrow(this::rejectCredentials);
        String token = tokenIssuer.issue(account.username(), account.role());
        return new Session(token, TokenIssuer.TTL_SECONDS,
                new AuthenticatedUser(account.username(), account.displayName(), account.role()));
    }

    private AuthException rejectCredentials() {
        LOG.info("Login rejected");
        return new AuthException(UNAUTHORIZED, INVALID_CREDENTIALS);
    }

    private static void requireText(String value, String message) {
        if (value == null || value.trim().isEmpty()) {
            throw new AuthException(BAD_REQUEST, message);
        }
    }
}
