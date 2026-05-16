package com.streakstudy.infrastructure.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Configuracion base de seguridad para SETUP-001.
 *
 * <p>Esta configuracion habilita el arranque limpio de la app y abre los endpoints
 * minimos necesarios para verificar que el proyecto levanta:
 * <ul>
 *   <li>{@code /actuator/health} — health checks (Docker / orquestadores).</li>
 *   <li>{@code /api/health} — endpoint propio para smoke test.</li>
 * </ul>
 *
 * <p>Las tareas AUTH-xxx posteriores anadiran filtros JWT y reglas mas estrictas.
 * Hasta entonces, el resto de endpoints requieren autenticacion (deny-by-default).</p>
 */
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/actuator/health/**",
                    "/actuator/info",
                    "/api/health"
                ).permitAll()
                .anyRequest().authenticated()
            )
            .httpBasic(AbstractHttpConfigurer::disable)
            .formLogin(AbstractHttpConfigurer::disable);
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
