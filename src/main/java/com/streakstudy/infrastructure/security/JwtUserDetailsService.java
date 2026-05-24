package com.streakstudy.infrastructure.security;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.streakstudy.domain.model.User;
import com.streakstudy.domain.repository.UserRepository;

@Service
public class JwtUserDetailsService implements UserDetailsService {

    private final UserRepository users;

    public JwtUserDetailsService(UserRepository users) {
        this.users = users;
    }

    @Override
    public UserDetails loadUserByUsername(String email) {
        User user = users.findByEmail(email.toLowerCase())
            .orElseThrow(() -> new UsernameNotFoundException(email));
        return AuthenticatedUserPrincipal.from(user);
    }

    public AuthenticatedUserPrincipal loadUserById(Long userId) {
        User user = users.findById(userId)
            .orElseThrow(() -> new UsernameNotFoundException(String.valueOf(userId)));
        return AuthenticatedUserPrincipal.from(user);
    }
}
