package com.streakstudy.application.port;

/**
 * Puerto para hashing de contrasenas. La implementacion vive en
 * {@code infrastructure.security} (BCrypt).
 */
public interface PasswordHasher {
    String hash(String rawPassword);
    boolean matches(String rawPassword, String passwordHash);
}
