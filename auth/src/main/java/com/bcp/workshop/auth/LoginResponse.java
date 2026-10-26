package com.bcp.workshop.auth;

public record LoginResponse(String token, long expiresIn, UserView user, String debug) {
    public record UserView(String username, String displayName, String role) {}
}
