package com.bcp.workshop.auth.security;

import jakarta.enterprise.context.ApplicationScoped;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

@ApplicationScoped
public class PasswordHasher {
    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final String SCHEME = "pbkdf2_sha256";
    private static final String SEPARATOR = "$";
    private static final int ITERATIONS = 600_000;
    private static final int SALT_BYTES = 16;
    private static final int KEY_BITS = 256;
    private static final int STORED_PARTS = 4;

    private final SecureRandom random = new SecureRandom();

    public String hash(String password) {
        byte[] salt = new byte[SALT_BYTES];
        random.nextBytes(salt);
        byte[] key = derive(password, salt, ITERATIONS);
        return String.join(SEPARATOR,
                SCHEME,
                String.valueOf(ITERATIONS),
                Base64.getEncoder().encodeToString(salt),
                Base64.getEncoder().encodeToString(key));
    }

    public boolean matches(String password, String stored) {
        String[] parts = stored.split("\\$", -1);
        if (parts.length != STORED_PARTS || !SCHEME.equals(parts[0])) {
            return false;
        }
        try {
            byte[] salt = Base64.getDecoder().decode(parts[2]);
            byte[] expected = Base64.getDecoder().decode(parts[3]);
            byte[] actual = derive(password, salt, Integer.parseInt(parts[1]));
            return MessageDigest.isEqual(expected, actual);
        } catch (IllegalArgumentException malformed) {
            return false;
        }
    }

    private static byte[] derive(String password, byte[] salt, int iterations) {
        try {
            SecretKeyFactory factory = SecretKeyFactory.getInstance(ALGORITHM);
            return factory.generateSecret(new PBEKeySpec(password.toCharArray(), salt, iterations, KEY_BITS)).getEncoded();
        } catch (GeneralSecurityException unavailable) {
            throw new IllegalStateException("PBKDF2 is not available", unavailable);
        }
    }
}
