package com.streakstudy.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import com.streakstudy.domain.model.User;
import com.streakstudy.domain.model.UserRole;

import io.jsonwebtoken.JwtException;

@ExtendWith(MockitoExtension.class)
class JwtServiceTest {

    private JwtService service;

    @BeforeEach
    void setUp() {
        JwtProperties props = new JwtProperties();
        props.setSecret("test-secret-test-secret-test-secret-test-secret"); // 48 chars
        props.setExpirationMs(60_000L);
        service = new JwtService(props);
    }

    @Test
    void issue_yParse_devuelvenLosMismosClaims() {
        User user = new User(1L, 1L, "alice@ute.com", "HASHED", "fullName", UserRole.STUDENT, Instant.now(), 0, 0);

        String token = service.issue(user);
        JwtService.ParsedToken parsed = service.parse(token);

        assertThat(parsed.userId()).isEqualTo(10L);
        assertThat(parsed.institutionId()).isEqualTo(7L);
        assertThat(parsed.role()).isEqualTo(UserRole.STUDENT);
        assertThat(parsed.email()).isEqualTo("alice@x.com");
    }

    @Test
    void parse_lanzaConTokenFirmadoConOtraClave() {
        // Generado por otra instancia con secreto distinto
        JwtProperties otraProps = new JwtProperties();
        otraProps.setSecret("OTRO-secret-OTRO-secret-OTRO-secret-OTRO-secret");
        otraProps.setExpirationMs(60_000L);
        JwtService otroService = new JwtService(otraProps);

        String tokenForaneo = otroService.issue(
            new User(1L, 1L, "x@x.com", "h", "X", UserRole.STUDENT, Instant.now(), 0, 0));

        assertThatThrownBy(() -> service.parse(tokenForaneo))
            .isInstanceOf(JwtException.class);
    }

    @Test
    void constructor_lanzaCuandoSecretEsMuyCorto() {
        JwtProperties badProps = new JwtProperties();
        badProps.setSecret("muy-corto");
        assertThatThrownBy(() -> new JwtService(badProps))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("256 bits");
    }

    @Test
    void expirationSeconds_devuelveExpirationMsDivididoEntre1000() {
        assertThat(service.expirationSeconds()).isEqualTo(60L);
    }
}
