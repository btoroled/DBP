package com.streakstudy.application.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.streakstudy.application.event.PasswordResetRequestedEvent;
import com.streakstudy.application.port.PasswordHasher;
import com.streakstudy.domain.exception.InvalidPasswordResetTokenException;
import com.streakstudy.domain.exception.PasswordResetTokenExpiredException;
import com.streakstudy.domain.model.User;
import com.streakstudy.domain.repository.UserRepository;
import com.streakstudy.infrastructure.persistence.entity.PasswordResetTokenJpa;
import com.streakstudy.infrastructure.persistence.repository.PasswordResetTokenJpaRepository;

/**
 * Servicio de recuperacion de contrasena. Mismo patron de hashing que
 * {@link RefreshTokenService}: el plaintext del token nunca se persiste.
 *
 * <p><b>Anti-enumeration:</b> {@code requestReset} responde igual ante un
 * email existente o inexistente. No se loguea email-not-found.</p>
 *
 * <p><b>Rotacion:</b> al emitir un token nuevo, los previos activos del
 * mismo usuario quedan invalidados (marcados {@code usedAt = now()}).</p>
 *
 * <p><b>Un solo uso:</b> {@code confirmReset} marca el token usado tras
 * rotear la password; un segundo intento devuelve 400.</p>
 */
@Service
public class PasswordResetService {

    private static final Base64.Encoder BASE64_URL_ENCODER = Base64.getUrlEncoder().withoutPadding();

    private final PasswordResetTokenJpaRepository tokens;
    private final UserRepository users;
    private final PasswordHasher passwordHasher;
    private final ApplicationEventPublisher eventPublisher;
    private final Duration tokenTtl;

    public PasswordResetService(PasswordResetTokenJpaRepository tokens,
                                UserRepository users,
                                PasswordHasher passwordHasher,
                                ApplicationEventPublisher eventPublisher,
                                @Value("${app.password-reset.token-ttl-minutes:30}") long tokenTtlMinutes) {
        this.tokens = tokens;
        this.users = users;
        this.passwordHasher = passwordHasher;
        this.eventPublisher = eventPublisher;
        this.tokenTtl = Duration.ofMinutes(tokenTtlMinutes);
    }

    @Transactional
    public void requestReset(String email) {
        if (email == null || email.isBlank()) {
            return; // anti-enumeration: misma respuesta ante input invalido
        }
        String normalized = email.toLowerCase();

        users.findByEmail(normalized).ifPresent(user -> {
            Instant now = Instant.now();
            tokens.invalidateAllActiveFor(user.id(), now);

            String plainToken = UUID.randomUUID() + "." + UUID.randomUUID();
            PasswordResetTokenJpa entity = new PasswordResetTokenJpa();
            entity.setUserId(user.id());
            entity.setTokenHash(hash(plainToken));
            entity.setExpiresAt(now.plus(tokenTtl));
            tokens.save(entity);

            eventPublisher.publishEvent(new PasswordResetRequestedEvent(
                    user.id(),
                    user.email(),
                    user.fullName(),
                    plainToken,
                    entity.getExpiresAt()));
        });
        // Si el email no existe, no se publica evento ni se loguea.
    }

    @Transactional
    public void confirmReset(String plainToken, String newPassword) {
        if (plainToken == null || plainToken.isBlank()) {
            throw new InvalidPasswordResetTokenException();
        }

        PasswordResetTokenJpa entity = tokens.findByTokenHash(hash(plainToken))
            .orElseThrow(InvalidPasswordResetTokenException::new);

        if (entity.getUsedAt() != null) {
            throw new InvalidPasswordResetTokenException();
        }
        if (entity.getExpiresAt().isBefore(Instant.now())) {
            throw new PasswordResetTokenExpiredException();
        }

        User user = users.findById(entity.getUserId())
            .orElseThrow(InvalidPasswordResetTokenException::new);

        users.save(user.withPasswordHash(passwordHasher.hash(newPassword)));

        entity.setUsedAt(Instant.now());
        tokens.save(entity);
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
}
