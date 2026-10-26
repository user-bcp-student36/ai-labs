package com.bcp.workshop.auth.service;

public record Session(String token, long expiresIn, AuthenticatedUser user) {}
