package com.streakstudy.infrastructure.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Propiedades de JWT bindeadas desde {@code application.properties}.
 *
 * <pre>
 * app.security.jwt.secret=...
 * app.security.jwt.expiration-ms=3600000
 * </pre>
 */
@ConfigurationProperties(prefix = "app.security.jwt")
public class JwtProperties {

    private String secret;
    private long expirationMs = 3_600_000L;

    public String getSecret() { return secret; }
    public void setSecret(String secret) { this.secret = secret; }

    public long getExpirationMs() { return expirationMs; }
    public void setExpirationMs(long expirationMs) { this.expirationMs = expirationMs; }
}
