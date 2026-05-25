package com.streakstudy.infrastructure.security;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.streakstudy.infrastructure.tenancy.TenantContext;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Filtro HTTP que:
 * <ol>
 *   <li>Lee el header {@code Authorization: Bearer <jwt>}.</li>
 *   <li>Valida el token con {@link JwtService}.</li>
 *   <li>Carga el usuario via {@link JwtUserDetailsService}.</li>
 *   <li>Popula {@code SecurityContext} con el principal autenticado.</li>
 *   <li>Popula {@link TenantContext} con el {@code institutionId} del claim.</li>
 *   <li>Limpia ambos al final del request (try/finally) para no contaminar
 *       el siguiente request servido por el mismo thread.</li>
 * </ol>
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private static final String AUTH_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final JwtUserDetailsService userDetailsService;

    public JwtAuthenticationFilter(JwtService jwtService, JwtUserDetailsService userDetailsService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String header = request.getHeader(AUTH_HEADER);
        boolean tenantSet = false;

        if (header != null && header.startsWith(BEARER_PREFIX)) {
            String token = header.substring(BEARER_PREFIX.length()).trim();
            try {
                JwtService.ParsedToken parsed = jwtService.parse(token);
                AuthenticatedUserPrincipal principal = userDetailsService.loadUserById(parsed.userId());

                var auth = new UsernamePasswordAuthenticationToken(
                    principal,
                    null,
                    principal.getAuthorities()
                );
                SecurityContextHolder.getContext().setAuthentication(auth);

                TenantContext.set(principal.institutionId());
                tenantSet = true;
            } catch (JwtException | IllegalArgumentException | UsernameNotFoundException ex) {
                log.warn("JWT auth failed: {}", ex.getMessage());
                SecurityContextHolder.clearContext();
            }
        }

        try {
            chain.doFilter(request, response);
        } finally {
            if (tenantSet) {
                TenantContext.clear();
            }
        }
    }
}
