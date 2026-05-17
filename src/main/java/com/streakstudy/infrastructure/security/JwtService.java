package com.streakstudy.infrastructure.security;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.stereotype.Service;

import com.streakstudy.application.port.TokenIssuer;
import com.streakstudy.domain.model.User;
import com.streakstudy.domain.model.UserRole;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

/**
 * Emite y valida JWTs HS256.
 *
 * <h2>Claims criticos</h2>
 * <ul>
 *   <li>{@code sub} — userId.</li>
 *   <li>{@code institutionId} — <b>clave del multi-tenancy</b>. El filtro
 *       {@link JwtAuthenticationFilter} lo usa para poblar el {@code TenantContext}.</li>
 *   <li>{@code role} — rol del usuario.</li>
 *   <li>{@code email} — para logs y verificacion.</li>
 * </ul>
 */
@Service
public class JwtService implements TokenIssuer {

    public static final String CLAIM_INSTITUTION = "institutionId";
    public static final String CLAIM_ROLE = "role";
    public static final String CLAIM_EMAIL = "email";

    private final JwtProperties props;
    private final SecretKey key;

    public JwtService(JwtProperties props) {
        this.props = props;
        if (props.getSecret() == null || props.getSecret().length() < 32) {
            throw new IllegalStateException(
                "app.security.jwt.secret debe tener al menos 32 caracteres (256 bits). "
              + "Configuralo en .env / variables de entorno.");
        }
        this.key = Keys.hmacShaKeyFor(props.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public String issue(User user) {
        Instant now = Instant.now();
        Instant exp = now.plusMillis(props.getExpirationMs());

        return Jwts.builder()
            .subject(String.valueOf(user.id()))
            .claim(CLAIM_INSTITUTION, user.institutionId())
            .claim(CLAIM_ROLE, user.role().name())
            .claim(CLAIM_EMAIL, user.email())
            .issuedAt(Date.from(now))
            .expiration(Date.from(exp))
            .signWith(key)
            .compact();
    }

    @Override
    public long expirationSeconds() {
        return props.getExpirationMs() / 1000;
    }

    /**
     * Parsea y valida el token. Lanza {@link JwtException} si la firma es
     * invalida o el token expiro.
     */
    public ParsedToken parse(String token) {
        Claims claims = Jwts.parser()
            .verifyWith(key)
            .build()
            .parseSignedClaims(token)
            .getPayload();

        Long userId = Long.parseLong(claims.getSubject());
        Long institutionId = claims.get(CLAIM_INSTITUTION, Number.class).longValue();
        UserRole role = UserRole.valueOf(claims.get(CLAIM_ROLE, String.class));
        String email = claims.get(CLAIM_EMAIL, String.class);

        return new ParsedToken(userId, institutionId, role, email);
    }

    public record ParsedToken(Long userId, Long institutionId, UserRole role, String email) { }
}
