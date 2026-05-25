package com.streakstudy.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import com.streakstudy.application.dto.RegisterRequest;
import com.streakstudy.application.event.UserRegisteredEvent;
import com.streakstudy.application.port.PasswordHasher;
import com.streakstudy.application.port.TokenIssuer;
import com.streakstudy.domain.exception.EmailAlreadyExistsException;
import com.streakstudy.domain.model.Institution;
import com.streakstudy.domain.model.User;
import com.streakstudy.domain.repository.InstitutionRepository;
import com.streakstudy.domain.repository.UserRepository;

/**
 * Verifica que AuthService publique UserRegisteredEvent en el flujo de
 * registro exitoso y NO lo publique cuando el registro falla.
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceEventTest {

    @Mock UserRepository users;
    @Mock InstitutionRepository institutions;
    @Mock PasswordHasher hasher;
    @Mock TokenIssuer tokens;
    @Mock RefreshTokenService refreshTokens;
    @Mock ApplicationEventPublisher events;
    @InjectMocks AuthService service;

    private final Institution inst = new Institution(1L, "UTEC", "utec", true, Instant.now());

    @Test
    void shouldPublishUserRegisteredEventWhenRegisterSucceeds() {
        when(institutions.findById(1L)).thenReturn(Optional.of(inst));
        when(users.existsByEmail("alice@utec.edu")).thenReturn(false);
        when(hasher.hash("Password123")).thenReturn("HASHED");
        when(users.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            return new User(10L, u.institutionId(), u.email(), u.passwordHash(), u.fullName(),
                    u.role(), u.createdAt(), 0, 0, null, 0, Set.of());
        });
        when(tokens.issue(any(User.class))).thenReturn("jwt");
        when(tokens.expirationSeconds()).thenReturn(3600L);
        when(refreshTokens.create(any(User.class))).thenReturn(
            new RefreshTokenService.RefreshTokenGrant("refresh", Instant.now().plus(30, ChronoUnit.DAYS)));

        service.register(new RegisterRequest(1L, "Alice@utec.edu", "Password123", "Alice Perez"));

        ArgumentCaptor<UserRegisteredEvent> captor = ArgumentCaptor.forClass(UserRegisteredEvent.class);
        verify(events).publishEvent(captor.capture());

        UserRegisteredEvent published = captor.getValue();
        assertThat(published.userId()).isEqualTo(10L);
        assertThat(published.institutionId()).isEqualTo(1L);
        assertThat(published.email()).isEqualTo("alice@utec.edu");
        assertThat(published.fullName()).isEqualTo("Alice Perez");
    }

    @Test
    void shouldNotPublishEventWhenEmailAlreadyExists() {
        when(institutions.findById(1L)).thenReturn(Optional.of(inst));
        when(users.existsByEmail("dup@utec.edu")).thenReturn(true);

        assertThatThrownBy(() -> service.register(
                new RegisterRequest(1L, "dup@utec.edu", "Password123", "Dup User")))
            .isInstanceOf(EmailAlreadyExistsException.class);

        verify(events, never()).publishEvent(any());
    }
}
