package com.streakstudy.application.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Base64;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.streakstudy.domain.exception.InvalidRefreshTokenException;
import com.streakstudy.domain.exception.RefreshTokenExpiredException;
import com.streakstudy.domain.exception.RefreshTokenRevokedException;
import com.streakstudy.domain.model.User;
import com.streakstudy.domain.repository.UserRepository;
import com.streakstudy.infrastructure.persistence.entity.RefreshTokenJpa;
import com.streakstudy.infrastructure.persistence.repository.RefreshTokenJpaRepository;
import com.streakstudy.infrastructure.security.JwtProperties;

@Service
public class RefreshTokenService {

    private static final Base64.Encoder BASE64_URL_ENCODER = Base64.getUrlEncoder().withoutPadding();

    private final RefreshTokenJpaRepository refreshTokens;
    private final UserRepository users;
    private final JwtProperties jwtProperties;

    public RefreshTokenService(RefreshTokenJpaRepository refreshTokens,
                               UserRepository users,
                               JwtProperties jwtProperties) {
        this.refreshTokens = refreshTokens;
        this.users = users;
        this.jwtProperties = jwtProperties;
    }

    @Transactional
    public RefreshTokenGrant create(User user) {
        String rawToken = java.util.UUID.randomUUID() + "." + java.util.UUID.randomUUID();

        RefreshTokenJpa entity = new RefreshTokenJpa();
        entity.setUserId(user.id());
        entity.setTokenHash(hash(rawToken));
        entity.setExpiresAt(Instant.now().plusMillis(jwtProperties.getRefreshExpirationMs()));
        entity.setRevoked(false);
        refreshTokens.save(entity);

        return new RefreshTokenGrant(rawToken, entity.getExpiresAt());
    }

    @Transactional(readOnly = true)
    public RefreshTokenSession validate(String rawToken) {
        RefreshTokenJpa entity = validateEntity(rawToken);
        return new RefreshTokenSession(entity.getId(), entity.getUserId(), entity.getExpiresAt(), entity.isRevoked());
    }

    @Transactional
    public void revoke(String rawToken) {
        RefreshTokenJpa entity = validateEntity(rawToken);
        entity.setRevoked(true);
        refreshTokens.save(entity);
    }

    @Transactional
    public RefreshTokenRotation rotate(String rawToken) {
        RefreshTokenJpa current = validateEntity(rawToken);
        current.setRevoked(true);
        refreshTokens.save(current);

        User user = users.findById(current.getUserId())
            .orElseThrow(InvalidRefreshTokenException::new);
        RefreshTokenGrant next = create(user);
        return new RefreshTokenRotation(user, next.token(), next.expiresAt());
    }

    private RefreshTokenJpa validateEntity(String rawToken) {
        RefreshTokenJpa entity = refreshTokens.findByTokenHash(hash(rawToken))
            .orElseThrow(InvalidRefreshTokenException::new);

        if (entity.isRevoked()) {
            throw new RefreshTokenRevokedException();
        }
        if (entity.getExpiresAt().isBefore(Instant.now())) {
            throw new RefreshTokenExpiredException();
        }
        return entity;
    }

    private String hash(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return BASE64_URL_ENCODER.encodeToString(hashed);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 no esta disponible.", ex);
        }
    }

    public record RefreshTokenGrant(String token, Instant expiresAt) { }

    public record RefreshTokenSession(Long id, Long userId, Instant expiresAt, boolean revoked) { }

    public record RefreshTokenRotation(User user, String refreshToken, Instant expiresAt) { }
}
