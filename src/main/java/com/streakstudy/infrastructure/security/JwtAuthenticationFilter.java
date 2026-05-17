package com.streakstudy.infrastructure.security;

import java.io.IOException;
import java.util.List;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
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
 *   <li>Popula {@code SecurityContext} con el principal autenticado.</li>
 *   <li>Popula {@link TenantContext} con el {@code institutionId} del claim.</li>
 *   <li>Limpia ambos al final del request (try/finally) para no contaminar
 *       el siguiente request servido por el mismo thread.</li>
 * </ol>
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String AUTH_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
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
                AuthenticatedUserPrincipal principal = new AuthenticatedUserPrincipal(
                    parsed.userId(), parsed.institutionId(), parsed.email(), parsed.role());

                var auth = new UsernamePasswordAuthenticationToken(
                    principal,
                    null,
                    List.of(new SimpleGrantedAuthority("ROLE_" + parsed.role().name()))
                );
                SecurityContextHolder.getContext().setAuthentication(auth);

                TenantContext.set(parsed.institutionId());
                tenantSet = true;
            } catch (JwtException | IllegalArgumentException ex) {
                // Token invalido => seguimos sin auth. Spring Security devolvera 401
                // si la ruta requiere autenticacion.
                SecurityContextHolder.clearContext();
            }
        }

        try {
            chain.doFilter(request, response);
        } finally {
            if (tenantSet) {
                TenantContext.clear();
            }
            SecurityContextHolder.clearContext();
        }
    }
}
