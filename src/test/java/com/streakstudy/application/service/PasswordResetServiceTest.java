package com.streakstudy.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import com.streakstudy.application.event.PasswordResetRequestedEvent;
import com.streakstudy.application.port.PasswordHasher;
import com.streakstudy.domain.exception.InvalidPasswordResetTokenException;
import com.streakstudy.domain.exception.PasswordResetTokenExpiredException;
import com.streakstudy.domain.model.User;
import com.streakstudy.domain.model.UserRole;
import com.streakstudy.domain.repository.UserRepository;
import com.streakstudy.infrastructure.persistence.entity.PasswordResetTokenJpa;
import com.streakstudy.infrastructure.persistence.repository.PasswordResetTokenJpaRepository;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

    @Mock PasswordResetTokenJpaRepository tokens;
    @Mock UserRepository users;
    @Mock PasswordHasher passwordHasher;
    @Mock ApplicationEventPublisher events;

    private PasswordResetService service;

    private final User user = new User(10L, 1L, "alice@utec.edu", "OLD_HASH", "Alice Perez",
            UserRole.STUDENT, Instant.now(), 0, 0, LocalDate.now(), 0, Set.of());

    private PasswordResetService newService() {
        return new PasswordResetService(tokens, users, passwordHasher, events, 30L);
    }

    @Test
    void shouldPublishEventAndInvalidatePriorWhenRequestResetForExistingEmail() {
        service = newService();
        when(users.findByEmail("alice@utec.edu")).thenReturn(Optional.of(user));
        when(tokens.save(any(PasswordResetTokenJpa.class))).thenAnswer(inv -> inv.getArgument(0));

        service.requestReset("Alice@utec.edu");

        verify(tokens).invalidateAllActiveFor(anyLong(), any(Instant.class));

        ArgumentCaptor<PasswordResetTokenJpa> tokenCaptor = ArgumentCaptor.forClass(PasswordResetTokenJpa.class);
        verify(tokens).save(tokenCaptor.capture());
        PasswordResetTokenJpa saved = tokenCaptor.getValue();
        assertThat(saved.getUserId()).isEqualTo(10L);
        assertThat(saved.getTokenHash()).isNotBlank();
        assertThat(saved.getExpiresAt()).isAfter(Instant.now().plus(29, ChronoUnit.MINUTES));
        assertThat(saved.getUsedAt()).isNull();

        ArgumentCaptor<PasswordResetRequestedEvent> evCaptor = ArgumentCaptor.forClass(PasswordResetRequestedEvent.class);
        verify(events).publishEvent(evCaptor.capture());
        PasswordResetRequestedEvent ev = evCaptor.getValue();
        assertThat(ev.userId()).isEqualTo(10L);
        assertThat(ev.email()).isEqualTo("alice@utec.edu");
        assertThat(ev.plainToken()).isNotBlank();
        // El plain token nunca debe coincidir con el hash guardado.
        assertThat(ev.plainToken()).isNotEqualTo(saved.getTokenHash());
    }

    @Test
    void shouldNotPublishEventNorSaveWhenRequestResetForUnknownEmail() {
        service = newService();
        when(users.findByEmail("ghost@x.com")).thenReturn(Optional.empty());

        service.requestReset("ghost@x.com");

        verify(tokens, never()).save(any());
        verify(tokens, never()).invalidateAllActiveFor(anyLong(), any());
        verify(events, never()).publishEvent(any());
    }

    @Test
    void shouldNotPublishEventWhenEmailIsBlank() {
        service = newService();

        service.requestReset("   ");

        verify(users, never()).findByEmail(anyString());
        verify(events, never()).publishEvent(any());
    }

    @Test
    void shouldUpdatePasswordAndMarkTokenUsedWhenConfirmResetWithValidToken() {
        service = newService();
        String plain = "the-plain-token";
        PasswordResetTokenJpa stored = new PasswordResetTokenJpa();
        stored.setId(42L);
        stored.setUserId(10L);
        stored.setTokenHash(sha256(plain));
        stored.setExpiresAt(Instant.now().plus(10, ChronoUnit.MINUTES));
        when(tokens.findByTokenHash(stored.getTokenHash())).thenReturn(Optional.of(stored));
        when(users.findById(10L)).thenReturn(Optional.of(user));
        when(passwordHasher.hash("NewPassword1")).thenReturn("NEW_HASH");

        service.confirmReset(plain, "NewPassword1");

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(users).save(userCaptor.capture());
        assertThat(userCaptor.getValue().passwordHash()).isEqualTo("NEW_HASH");

        verify(tokens).save(stored);
        assertThat(stored.getUsedAt()).isNotNull();
    }

    @Test
    void shouldThrowInvalidTokenWhenConfirmResetWithUnknownToken() {
        service = newService();
        when(tokens.findByTokenHash(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.confirmReset("does-not-exist", "NewPassword1"))
            .isInstanceOf(InvalidPasswordResetTokenException.class);

        verify(users, never()).save(any());
    }

    @Test
    void shouldThrowInvalidTokenWhenConfirmResetWithAlreadyUsedToken() {
        service = newService();
        String plain = "used-token";
        PasswordResetTokenJpa stored = new PasswordResetTokenJpa();
        stored.setUserId(10L);
        stored.setTokenHash(sha256(plain));
        stored.setExpiresAt(Instant.now().plus(10, ChronoUnit.MINUTES));
        stored.setUsedAt(Instant.now().minus(1, ChronoUnit.MINUTES));
        when(tokens.findByTokenHash(stored.getTokenHash())).thenReturn(Optional.of(stored));

        assertThatThrownBy(() -> service.confirmReset(plain, "NewPassword1"))
            .isInstanceOf(InvalidPasswordResetTokenException.class);

        verify(users, never()).save(any());
    }

    @Test
    void shouldThrowExpiredWhenConfirmResetWithExpiredToken() {
        service = newService();
        String plain = "expired-token";
        PasswordResetTokenJpa stored = new PasswordResetTokenJpa();
        stored.setUserId(10L);
        stored.setTokenHash(sha256(plain));
        stored.setExpiresAt(Instant.now().minus(1, ChronoUnit.MINUTES));
        when(tokens.findByTokenHash(stored.getTokenHash())).thenReturn(Optional.of(stored));

        assertThatThrownBy(() -> service.confirmReset(plain, "NewPassword1"))
            .isInstanceOf(PasswordResetTokenExpiredException.class);

        verify(users, never()).save(any());
    }

    @Test
    void shouldThrowInvalidTokenWhenConfirmResetCalledWithBlankToken() {
        service = newService();

        assertThatThrownBy(() -> service.confirmReset("", "NewPassword1"))
            .isInstanceOf(InvalidPasswordResetTokenException.class);
    }

    /** SHA-256 + base64url(no-padding), igual que el service. */
    private static String sha256(String raw) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(raw.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(hashed);
        } catch (java.security.NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
