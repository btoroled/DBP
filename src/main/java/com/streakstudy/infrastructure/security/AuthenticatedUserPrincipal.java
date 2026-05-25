package com.streakstudy.infrastructure.security;

import java.util.Collection;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.streakstudy.domain.model.User;
import com.streakstudy.domain.model.UserRole;

/**
 * Principal almacenado en {@code Authentication.getPrincipal()} para requests
 * autenticados. Implementa {@link UserDetails} para integrarse con Spring
 * Security sin perder el contexto de negocio que usan los controllers.
 */
public final class AuthenticatedUserPrincipal implements UserDetails {

    private final Long userId;
    private final Long institutionId;
    private final String email;
    private final String passwordHash;
    private final UserRole role;
    private final List<GrantedAuthority> authorities;

    public AuthenticatedUserPrincipal(Long userId, Long institutionId, String email, UserRole role) {
        this(userId, institutionId, email, null, role);
    }

    public AuthenticatedUserPrincipal(Long userId,
                                      Long institutionId,
                                      String email,
                                      String passwordHash,
                                      UserRole role) {
        this.userId = userId;
        this.institutionId = institutionId;
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
        this.authorities = List.of(
            new SimpleGrantedAuthority(role.name()),
            new SimpleGrantedAuthority("ROLE_" + role.name())
        );
    }

    public static AuthenticatedUserPrincipal from(User user) {
        return new AuthenticatedUserPrincipal(
            user.id(),
            user.institutionId(),
            user.email(),
            user.passwordHash(),
            user.role()
        );
    }

    public Long userId() { return userId; }
    public Long institutionId() { return institutionId; }
    public String email() { return email; }
    public UserRole role() { return role; }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
