package com.streakstudy.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Test unitario de SecurityConfig.
 *
 * <p>Validamos que el bean {@code PasswordEncoder} es un BCrypt y codifica
 * correctamente, sin levantar el contexto de Spring. Se usa
 * {@link MockitoExtension} para mantener la convencion del proyecto: <b>todos
 * los tests de unidad se ejecutan bajo la extension de Mockito</b> aunque
 * no se mockee nada en este caso particular.</p>
 */
@ExtendWith(MockitoExtension.class)
class SecurityConfigTest {

    private final JwtAuthenticationFilter jwtFilter = mock(JwtAuthenticationFilter.class);

    private final SecurityConfig config = new SecurityConfig(jwtFilter);

    @Test
    void passwordEncoder_codificaYValidaContrasenasCorrectamente() {
        PasswordEncoder encoder = config.passwordEncoder();

        String raw = "mi-password-segura";
        String hashed = encoder.encode(raw);

        assertThat(hashed).isNotEqualTo(raw);
        assertThat(encoder.matches(raw, hashed)).isTrue();
        assertThat(encoder.matches("otra-password", hashed)).isFalse();
    }

    @Test
    void passwordEncoder_generaHashesDiferentesParaLaMismaEntrada() {
        PasswordEncoder encoder = config.passwordEncoder();

        String raw = "duplicate-input";
        String hashA = encoder.encode(raw);
        String hashB = encoder.encode(raw);

        // BCrypt usa salt aleatorio: hashes deben diferir, pero ambos validan.
        assertThat(hashA).isNotEqualTo(hashB);
        assertThat(encoder.matches(raw, hashA)).isTrue();
        assertThat(encoder.matches(raw, hashB)).isTrue();
    }
}
