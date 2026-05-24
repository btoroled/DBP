package com.streakstudy.application.service;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.streakstudy.application.dto.AuthResponse;
import com.streakstudy.application.dto.LoginRequest;
import com.streakstudy.application.dto.RegisterRequest;
import com.streakstudy.application.event.UserRegisteredEvent;
import com.streakstudy.application.port.PasswordHasher;
import com.streakstudy.application.port.TokenIssuer;
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
    private final ApplicationEventPublisher eventPublisher;

    public AuthService(UserRepository users,
                       InstitutionRepository institutions,
                       PasswordHasher passwordHasher,
                       TokenIssuer tokenIssuer,
                       ApplicationEventPublisher eventPublisher) {
        this.users = users;
        this.institutions = institutions;
        this.passwordHasher = passwordHasher;
        this.tokenIssuer = tokenIssuer;
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

        return AuthResponse.of(tokenIssuer.issue(saved), tokenIssuer.expirationSeconds(), saved);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest req) {
        User user = users.findByEmail(req.email().toLowerCase())
            .orElseThrow(InvalidCredentialsException::new);

        if (!passwordHasher.matches(req.password(), user.passwordHash())) {
            throw new InvalidCredentialsException();
        }

        return AuthResponse.of(tokenIssuer.issue(user), tokenIssuer.expirationSeconds(), user);
    }
}
