package com.streakstudy.infrastructure.security;

import java.util.List;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Configuracion de seguridad: JWT stateless + multi-tenant.
 *
 * <h2>Endpoints publicos (no requieren auth)</h2>
 * <ul>
 *   <li>{@code /actuator/health/**}, {@code /actuator/info}</li>
 *   <li>{@code /api/health}</li>
 *   <li>{@code /api/v1/auth/**} (register, login)</li>
 *   <li>{@code POST /api/v1/institutions} y {@code GET /api/v1/institutions/**}
 *       — cross-tenant (en produccion deberian limitarse a SUPER_ADMIN).</li>
 * </ul>
 *
 * <p>El resto exige JWT valido. {@link JwtAuthenticationFilter} corre antes que
 * {@code UsernamePasswordAuthenticationFilter} para poblar el contexto.</p>
 *
 * <p>CORS habilitado para frontend local (Vite 5173, CRA 3000) y la URL de
 * deployment (env {@code FRONTEND_URL}).</p>
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
            .cors(Customizer.withDefaults())
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                    .requestMatchers(
                            "/actuator/health/**",
                            "/actuator/info",
                            "/api/health",
                            "/swagger-ui/**",
                            "/swagger-ui.html",
                            "/webjars/**",
                            "/v3/api-docs/**"
                    ).permitAll()
                .requestMatchers("/api/v1/auth/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/v1/institutions").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/institutions/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/v1/users/me/progress/review").hasAuthority("STUDENT")
                .anyRequest().authenticated()
            )
            .httpBasic(AbstractHttpConfigurer::disable)
            .formLogin(AbstractHttpConfigurer::disable)
            .userDetailsService(jwtUserDetailsService)
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration cfg = new CorsConfiguration();
        String frontendUrl = System.getenv().getOrDefault("FRONTEND_URL", "https://streakstudy.example.com");
        cfg.setAllowedOrigins(List.of(
            "http://localhost:5173",
            "http://localhost:3000",
            frontendUrl
        ));
        cfg.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        cfg.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        cfg.setExposedHeaders(List.of("Authorization"));
        cfg.setAllowCredentials(true);
        cfg.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cfg);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
