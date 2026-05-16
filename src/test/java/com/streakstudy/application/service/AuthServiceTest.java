package com.streakstudy.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.streakstudy.application.dto.AuthResponse;
import com.streakstudy.application.dto.LoginRequest;
import com.streakstudy.application.dto.RegisterRequest;
import com.streakstudy.application.port.PasswordHasher;
import com.streakstudy.application.port.TokenIssuer;
import com.streakstudy.domain.exception.EmailAlreadyExistsException;
import com.streakstudy.domain.exception.EntityNotFoundException;
import com.streakstudy.domain.exception.InvalidCredentialsException;
import com.streakstudy.domain.model.Institution;
import com.streakstudy.domain.model.User;
import com.streakstudy.domain.model.UserRole;
import com.streakstudy.domain.repository.InstitutionRepository;
import com.streakstudy.domain.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock UserRepository users;
    @Mock InstitutionRepository institutions;
    @Mock PasswordHasher hasher;
    @Mock TokenIssuer tokens;

    @InjectMocks AuthService service;

    private final Institution inst = new Institution(1L, "UTEC", "utec", true, Instant.now());

    @Test
    void register_creaUsuarioConInstitutionIdYDevuelveToken() {
        RegisterRequest req = new RegisterRequest(1L, "Alice@utec.edu", "Password123", "Alice");
        when(institutions.findById(1L)).thenReturn(Optional.of(inst));
        when(users.existsByEmail("alice@utec.edu")).thenReturn(false);
        when(hasher.hash("Password123")).thenReturn("HASHED");
        when(users.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            return new User(10L, u.institutionId(), u.email(), u.passwordHash(), u.fullName(), u.role(), Instant.now());
        });
        when(tokens.issue(any(User.class))).thenReturn("jwt-token");
        when(tokens.expirationSeconds()).thenReturn(3600L);

        AuthResponse resp = service.register(req);

        assertThat(resp.token()).isEqualTo("jwt-token");
        assertThat(resp.expiresInSeconds()).isEqualTo(3600L);
        assertThat(resp.institutionId()).isEqualTo(1L);
        assertThat(resp.email()).isEqualTo("alice@utec.edu"); // normalizado
        assertThat(resp.role()).isEqualTo(UserRole.STUDENT);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(users).save(captor.capture());
        User saved = captor.getValue();
        assertThat(saved.institutionId()).isEqualTo(1L);
        assertThat(saved.passwordHash()).isEqualTo("HASHED");
        assertThat(saved.role()).isEqualTo(UserRole.STUDENT);
    }

    @Test
    void register_lanzaSiInstitutionNoExiste() {
        when(institutions.findById(99L)).thenReturn(Optional.empty());
        RegisterRequest req = new RegisterRequest(99L, "a@b.com", "Password123", "A");

        assertThatThrownBy(() -> service.register(req))
            .isInstanceOf(EntityNotFoundException.class)
            .hasMessageContaining("Institution");
        verify(users, never()).save(any());
    }

    @Test
    void register_lanzaSiEmailYaExiste() {
        when(institutions.findById(1L)).thenReturn(Optional.of(inst));
        when(users.existsByEmail("a@b.com")).thenReturn(true);

        assertThatThrownBy(() -> service.register(new RegisterRequest(1L, "a@b.com", "Password123", "A")))
            .isInstanceOf(EmailAlreadyExistsException.class);
        verify(users, never()).save(any());
    }

    @Test
    void login_devuelveTokenCuandoCredencialesSonCorrectas() {
        User existing = new User(10L, 1L, "alice@utec.edu", "HASHED", "Alice", UserRole.STUDENT, Instant.now());
        when(users.findByEmail("alice@utec.edu")).thenReturn(Optional.of(existing));
        when(hasher.matches("Password123", "HASHED")).thenReturn(true);
        when(tokens.issue(existing)).thenReturn("jwt-token");
        when(tokens.expirationSeconds()).thenReturn(3600L);

        AuthResponse resp = service.login(new LoginRequest("Alice@utec.edu", "Password123"));

        assertThat(resp.token()).isEqualTo("jwt-token");
        assertThat(resp.institutionId()).isEqualTo(1L);
        verify(tokens).issue(existing);
    }

    @Test
    void login_lanzaCuandoEmailNoExiste() {
        when(users.findByEmail("ghost@x.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.login(new LoginRequest("ghost@x.com", "x")))
            .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void login_lanzaCuandoPasswordEsIncorrecta() {
        User existing = new User(10L, 1L, "alice@utec.edu", "HASHED", "Alice", UserRole.STUDENT, Instant.now());
        when(users.findByEmail("alice@utec.edu")).thenReturn(Optional.of(existing));
        when(hasher.matches(eq("wrong"), anyString())).thenReturn(false);

        assertThatThrownBy(() -> service.login(new LoginRequest("alice@utec.edu", "wrong")))
            .isInstanceOf(InvalidCredentialsException.class);
        verify(tokens, never()).issue(any());
    }
}
