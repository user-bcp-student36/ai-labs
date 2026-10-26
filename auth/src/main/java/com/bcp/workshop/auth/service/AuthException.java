package com.bcp.workshop.auth.service;

import jakarta.ws.rs.core.Response;

public class AuthException extends RuntimeException {
    private final Response.Status status;

    public AuthException(Response.Status status, String message) {
        super(message);
        this.status = status;
    }

    public Response.Status status() {
        return status;
    }
}
