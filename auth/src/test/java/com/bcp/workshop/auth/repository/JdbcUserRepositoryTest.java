package com.bcp.workshop.auth.repository;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JdbcUserRepositoryTest {
    @TempDir
    Path folder;

    @BeforeAll
    static void loadSqliteDriver() throws ClassNotFoundException {
        Class.forName("org.sqlite.JDBC");
    }

    @Test
    void storesAndFindsUserWithoutLoadingOtherRows() {
        JdbcUserRepository repository = repositoryInFolder();
        repository.createSchema();
        repository.addIfAbsent("ana", "hash-ana", "Ana Prueba", "STUDENT");
        repository.addIfAbsent("luis", "hash-luis", "Luis Prueba", "TRAINER");

        UserAccount account = repository.findByUsername("ana").orElseThrow();

        assertEquals("ana", account.username());
        assertEquals("hash-ana", account.passwordHash());
        assertEquals("Ana Prueba", account.displayName());
        assertEquals("STUDENT", account.role());
    }

    @Test
    void addIfAbsentKeepsExistingUser() {
        JdbcUserRepository repository = repositoryInFolder();
        repository.createSchema();
        repository.addIfAbsent("ana", "primero", "Ana Prueba", "STUDENT");
        repository.addIfAbsent("ana", "segundo", "Otra", "TRAINER");

        assertEquals("primero", repository.findByUsername("ana").orElseThrow().passwordHash());
    }

    @Test
    void returnsEmptyForUnknownOrQuotedUsername() {
        JdbcUserRepository repository = repositoryInFolder();
        repository.createSchema();

        assertFalse(repository.findByUsername("nadie").isPresent());
        assertTrue(repository.findByUsername("O'Neil").isEmpty());
    }

    private JdbcUserRepository repositoryInFolder() {
        return new JdbcUserRepository("jdbc:sqlite:" + folder.resolve("nested").resolve("users.db").toString().replace('\\', '/'));
    }
}
