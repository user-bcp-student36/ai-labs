package com.bcp.workshop.auth.repository;

public record UserAccount(long id, String username, String passwordHash, String displayName, String role) {}
