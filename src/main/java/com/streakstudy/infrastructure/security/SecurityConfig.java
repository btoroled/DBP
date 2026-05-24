package com.streakstudy.infrastructure.security;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Configuracion de seguridad: JWT stateless + multi-tenant.
 *
 * <h2>Endpoints publicos (no requieren auth)</h2>
 * <ul>
 *   <li>{@code /actuator/health/**}, {@code /actuator/info}</li>
 *   <li>{@code /api/health}</li>
 *   <li>{@code /api/auth/register}, {@code /api/auth/login}, {@code /api/auth/refresh}
 *       y sus aliases en {@code /api/v1/auth/**}</li>
 *   <li>{@code POST /api/institutions} y {@code GET /api/institutions/**}
 *       — cross-tenant (en produccion deberian limitarse a SUPER_ADMIN).</li>
 * </ul>
 *
 * <p>El resto exige JWT valido. {@link JwtAuthenticationFilter} corre antes que
 * {@code UsernamePasswordAuthenticationFilter} para poblar el contexto.</p>
 */
@Configuration
@EnableConfigurationProperties(JwtProperties.class)
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtFilter;
    private final JwtUserDetailsService jwtUserDetailsService;

    public SecurityConfig(JwtAuthenticationFilter jwtFilter,
                          JwtUserDetailsService jwtUserDetailsService) {
        this.jwtFilter = jwtFilter;
        this.jwtUserDetailsService = jwtUserDetailsService;
    }

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
                .requestMatchers(
                    "/api/auth/register",
                    "/api/auth/login",
                    "/api/auth/refresh",
                    "/api/v1/auth/register",
                    "/api/v1/auth/login",
                    "/api/v1/auth/refresh"
                ).permitAll()
                .requestMatchers(HttpMethod.POST, "/api/institutions").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/institutions/**").permitAll()
                    .requestMatchers(HttpMethod.POST, "/api/users/me/progress/review").hasAuthority("STUDENT")
                .anyRequest().authenticated()
            )
            .httpBasic(AbstractHttpConfigurer::disable)
            .formLogin(AbstractHttpConfigurer::disable)
            .userDetailsService(jwtUserDetailsService)
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
