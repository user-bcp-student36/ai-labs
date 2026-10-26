package com.bcp.workshop.auth.repository;

import java.util.Optional;

public interface UserRepository {
    void createSchema();

    void addIfAbsent(String username, String passwordHash, String displayName, String role);

    Optional<UserAccount> findByUsername(String username);
}
