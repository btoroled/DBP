package com.streakstudy.application.service;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.streakstudy.application.dto.AuthResponse;
import com.streakstudy.application.dto.LoginRequest;
import com.streakstudy.application.dto.RefreshTokenRequest;
import com.streakstudy.application.dto.RegisterRequest;
import com.streakstudy.application.event.UserRegisteredEvent;
import com.streakstudy.application.port.PasswordHasher;
import com.streakstudy.application.port.TokenIssuer;
import com.streakstudy.domain.exception.InvalidRefreshTokenException;
import com.streakstudy.domain.exception.EmailAlreadyExistsException;
import com.streakstudy.domain.exception.EntityNotFoundException;
import com.streakstudy.domain.exception.InvalidCredentialsException;
import com.streakstudy.domain.model.User;
import com.streakstudy.domain.repository.InstitutionRepository;
import com.streakstudy.domain.repository.UserRepository;

/**
 * Servicio de autenticacion. Cross-tenant a proposito:
 *
 * <ul>
 *   <li>Registro: el cliente declara su {@code institutionId}.</li>
 *   <li>Login: el cliente solo da email; resolvemos el tenant a partir del usuario.</li>
 * </ul>
 *
 * <p>Una vez emitido el JWT, el cliente envia {@code Authorization: Bearer ...}
 * en cada request, y {@code JwtAuthenticationFilter} setea el {@code TenantContext}.</p>
 */
@Service
public class AuthService {

    private final UserRepository users;
    private final InstitutionRepository institutions;
    private final PasswordHasher passwordHasher;
    private final TokenIssuer tokenIssuer;
    private final RefreshTokenService refreshTokenService;
    private final ApplicationEventPublisher eventPublisher;

    public AuthService(UserRepository users,
                       InstitutionRepository institutions,
                       PasswordHasher passwordHasher,
                       TokenIssuer tokenIssuer,
                       RefreshTokenService refreshTokenService,
                       ApplicationEventPublisher eventPublisher) {
        this.users = users;
        this.institutions = institutions;
        this.passwordHasher = passwordHasher;
        this.tokenIssuer = tokenIssuer;
        this.refreshTokenService = refreshTokenService;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public AuthResponse register(RegisterRequest req) {
        String email = req.email().toLowerCase();

        // Verificacion explicita de existencia de institucion para mensaje claro.
        institutions.findById(req.institutionId())
            .orElseThrow(() -> new EntityNotFoundException("Institution", req.institutionId()));

        if (users.existsByEmail(email)) {
            throw new EmailAlreadyExistsException(email);
        }

        User toSave = User.newStudent(
            req.institutionId(),
            email,
            passwordHasher.hash(req.password()),
            req.fullName()
        );
        User saved = users.save(toSave);

        eventPublisher.publishEvent(new UserRegisteredEvent(
                saved.id(), saved.institutionId(), saved.email(), saved.fullName()));

        return issueTokens(saved);
    }

    @Transactional
    public AuthResponse login(LoginRequest req) {
        User user = users.findByEmail(req.email().toLowerCase())
            .orElseThrow(InvalidCredentialsException::new);

        if (!passwordHasher.matches(req.password(), user.passwordHash())) {
            throw new InvalidCredentialsException();
        }

        return issueTokens(user);
    }

    @Transactional
    public AuthResponse refresh(RefreshTokenRequest req) {
        RefreshTokenService.RefreshTokenRotation rotation = refreshTokenService.rotate(req.refreshToken());
        return AuthResponse.of(
            tokenIssuer.issue(rotation.user()),
            rotation.refreshToken(),
            tokenIssuer.expirationSeconds(),
            rotation.user()
        );
    }

    @Transactional
    public void logout(Long userId, RefreshTokenRequest req) {
        RefreshTokenService.RefreshTokenSession session = refreshTokenService.validate(req.refreshToken());
        if (!session.userId().equals(userId)) {
            throw new InvalidRefreshTokenException();
        }
        refreshTokenService.revoke(req.refreshToken());
    }

    private AuthResponse issueTokens(User user) {
        RefreshTokenService.RefreshTokenGrant refreshToken = refreshTokenService.create(user);
        return AuthResponse.of(
            tokenIssuer.issue(user),
            refreshToken.token(),
            tokenIssuer.expirationSeconds(),
            user
        );
    }
}
