package com.streakstudy.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.streakstudy.application.dto.AuthResponse;
import com.streakstudy.application.dto.RefreshTokenRequest;
import com.streakstudy.application.port.PasswordHasher;
import com.streakstudy.application.port.TokenIssuer;
import com.streakstudy.domain.exception.InvalidRefreshTokenException;
import com.streakstudy.domain.exception.RefreshTokenExpiredException;
import com.streakstudy.domain.exception.RefreshTokenRevokedException;
import com.streakstudy.domain.model.Institution;
import com.streakstudy.domain.model.User;
import com.streakstudy.domain.model.UserRole;
import com.streakstudy.domain.repository.InstitutionRepository;
import com.streakstudy.domain.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class AuthServiceRefreshTest {

    @Mock UserRepository users;
    @Mock InstitutionRepository institutions;
    @Mock PasswordHasher hasher;
    @Mock TokenIssuer tokens;
    @Mock RefreshTokenService refreshTokens;

    @InjectMocks AuthService service;

    @Test
    void shouldRotateRefreshTokenAndReturnNewAccessToken() {
        User user = user();
        RefreshTokenRequest request = new RefreshTokenRequest("refresh-token");
        when(refreshTokens.rotate("refresh-token")).thenReturn(
            new RefreshTokenService.RefreshTokenRotation(user, "refresh-token-2", Instant.now().plusSeconds(3600))
        );
        when(tokens.issue(user)).thenReturn("access-token-2");
        when(tokens.expirationSeconds()).thenReturn(900L);

        AuthResponse response = service.refresh(request);

        assertThat(response.accessToken()).isEqualTo("access-token-2");
        assertThat(response.refreshToken()).isEqualTo("refresh-token-2");
        assertThat(response.expiresIn()).isEqualTo(900L);
    }

    @Test
    void shouldPropagateExpiredRefreshTokenError() {
        when(refreshTokens.rotate("expired")).thenThrow(new RefreshTokenExpiredException());

        assertThatThrownBy(() -> service.refresh(new RefreshTokenRequest("expired")))
            .isInstanceOf(RefreshTokenExpiredException.class);
    }

    @Test
    void shouldPropagateRevokedRefreshTokenError() {
        when(refreshTokens.rotate("revoked")).thenThrow(new RefreshTokenRevokedException());

        assertThatThrownBy(() -> service.refresh(new RefreshTokenRequest("revoked")))
            .isInstanceOf(RefreshTokenRevokedException.class);
    }

    @Test
    void shouldRevokeRefreshTokenOnLogout() {
        when(refreshTokens.validate("refresh-token")).thenReturn(
            new RefreshTokenService.RefreshTokenSession(10L, 42L, Instant.now().plusSeconds(60), false)
        );

        service.logout(42L, new RefreshTokenRequest("refresh-token"));

        verify(refreshTokens).revoke("refresh-token");
    }

    @Test
    void shouldRejectLogoutWhenRefreshTokenBelongsToAnotherUser() {
        when(refreshTokens.validate("refresh-token")).thenReturn(
            new RefreshTokenService.RefreshTokenSession(10L, 99L, Instant.now().plusSeconds(60), false)
        );

        assertThatThrownBy(() -> service.logout(42L, new RefreshTokenRequest("refresh-token")))
            .isInstanceOf(InvalidRefreshTokenException.class);
    }

    private User user() {
        return new User(
            42L,
            7L,
            "alice@test.com",
            "HASH",
            "Alice",
            UserRole.STUDENT,
            Instant.now(),
            10,
            2,
            LocalDate.now(),
            0,
            Set.of()
        );
    }
}
