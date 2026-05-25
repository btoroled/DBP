package com.streakstudy.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import com.streakstudy.domain.model.User;
import com.streakstudy.domain.model.UserRole;
import com.streakstudy.domain.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class JwtUserDetailsServiceTest {

    @Mock UserRepository users;

    @Test
    void shouldLoadExistingUserByUsername() {
        User user = new User(
            10L,
            7L,
            "alice@test.com",
            "HASH",
            "Alice",
            UserRole.STUDENT,
            Instant.now(),
            0,
            0,
            LocalDate.now(),
            0,
            Set.of()
        );
        when(users.findByEmail("alice@test.com")).thenReturn(Optional.of(user));

        JwtUserDetailsService service = new JwtUserDetailsService(users);
        AuthenticatedUserPrincipal principal = (AuthenticatedUserPrincipal) service.loadUserByUsername("Alice@Test.com");

        assertThat(principal.userId()).isEqualTo(10L);
        assertThat(principal.institutionId()).isEqualTo(7L);
        assertThat(principal.getUsername()).isEqualTo("alice@test.com");
        assertThat(principal.getAuthorities()).extracting("authority")
            .contains("STUDENT", "ROLE_STUDENT");
    }

    @Test
    void shouldThrowWhenUserDoesNotExist() {
        when(users.findByEmail("ghost@test.com")).thenReturn(Optional.empty());

        JwtUserDetailsService service = new JwtUserDetailsService(users);

        assertThatThrownBy(() -> service.loadUserByUsername("ghost@test.com"))
            .isInstanceOf(UsernameNotFoundException.class)
            .hasMessageContaining("ghost@test.com");
    }
}
