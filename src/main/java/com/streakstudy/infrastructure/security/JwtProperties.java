package com.streakstudy.infrastructure.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Propiedades de JWT bindeadas desde {@code application.properties}.
 *
 * <pre>
 * app.security.jwt.secret=...
 * app.security.jwt.expiration-ms=900000
 * app.security.jwt.refresh-expiration-ms=2592000000
 * </pre>
 */
@ConfigurationProperties(prefix = "app.security.jwt")
public class JwtProperties {

    private String secret;
    private long expirationMs = 900_000L;
    private long refreshExpirationMs = 2_592_000_000L;

    public String getSecret() { return secret; }
    public void setSecret(String secret) { this.secret = secret; }

    public long getExpirationMs() { return expirationMs; }
    public void setExpirationMs(long expirationMs) { this.expirationMs = expirationMs; }

    public long getRefreshExpirationMs() { return refreshExpirationMs; }
    public void setRefreshExpirationMs(long refreshExpirationMs) { this.refreshExpirationMs = refreshExpirationMs; }
}
