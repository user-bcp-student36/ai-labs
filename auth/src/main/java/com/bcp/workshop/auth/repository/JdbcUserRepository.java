package com.bcp.workshop.auth.repository;

import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

@ApplicationScoped
public class JdbcUserRepository implements UserRepository {
    private static final String SQLITE_PREFIX = "jdbc:sqlite:";
    private static final String CREATE_SQL = "CREATE TABLE IF NOT EXISTS auth_users (id INTEGER PRIMARY KEY AUTOINCREMENT, username TEXT UNIQUE NOT NULL, password_hash TEXT NOT NULL, display_name TEXT NOT NULL, role TEXT NOT NULL)";
    private static final String INSERT_SQL = "INSERT OR IGNORE INTO auth_users(username, password_hash, display_name, role) VALUES (?, ?, ?, ?)";
    private static final String FIND_SQL = "SELECT id, username, password_hash, display_name, role FROM auth_users WHERE username = ?";

    private final String jdbcUrl;

    public JdbcUserRepository(@ConfigProperty(name = "auth.db.url") String jdbcUrl) {
        this.jdbcUrl = jdbcUrl;
    }

    @Override
    public void createSchema() {
        createParentDirectory();
        try (Connection connection = open(); PreparedStatement statement = connection.prepareStatement(CREATE_SQL)) {
            statement.executeUpdate();
        } catch (SQLException error) {
            throw new IllegalStateException("Failed to create auth schema", error);
        }
    }

    @Override
    public void addIfAbsent(String username, String passwordHash, String displayName, String role) {
        try (Connection connection = open(); PreparedStatement statement = connection.prepareStatement(INSERT_SQL)) {
            statement.setString(1, username);
            statement.setString(2, passwordHash);
            statement.setString(3, displayName);
            statement.setString(4, role);
            statement.executeUpdate();
        } catch (SQLException error) {
            throw new IllegalStateException("Failed to insert auth user", error);
        }
    }

    @Override
    public Optional<UserAccount> findByUsername(String username) {
        try (Connection connection = open(); PreparedStatement statement = connection.prepareStatement(FIND_SQL)) {
            statement.setString(1, username);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    return Optional.empty();
                }
                return Optional.of(new UserAccount(
                        result.getLong("id"),
                        result.getString("username"),
                        result.getString("password_hash"),
                        result.getString("display_name"),
                        result.getString("role")));
            }
        } catch (SQLException error) {
            throw new IllegalStateException("Failed to query auth user", error);
        }
    }

    private Connection open() throws SQLException {
        return DriverManager.getConnection(jdbcUrl);
    }

    private void createParentDirectory() {
        if (!jdbcUrl.startsWith(SQLITE_PREFIX)) {
            return;
        }
        Path file = Path.of(jdbcUrl.substring(SQLITE_PREFIX.length())).toAbsolutePath();
        try {
            Files.createDirectories(file.getParent());
        } catch (IOException error) {
            throw new IllegalStateException("Failed to create database directory", error);
        }
    }
}
