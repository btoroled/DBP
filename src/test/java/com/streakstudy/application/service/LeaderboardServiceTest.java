package com.streakstudy.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.streakstudy.application.dto.LeaderboardUserResponse;
import com.streakstudy.domain.exception.TenantViolationException;
import com.streakstudy.domain.model.User;
import com.streakstudy.domain.model.UserRole;
import com.streakstudy.domain.repository.UserRepository;
import com.streakstudy.infrastructure.tenancy.TenantContext;

@ExtendWith(MockitoExtension.class)
class LeaderboardServiceTest {

    @Mock UserRepository userRepository;
    @InjectMocks LeaderboardService service;

    @AfterEach
    void cleanup() {
        TenantContext.clear();
    }

    @Test
    void shouldUseTenantContextAndMapResultWhenGettingLeaderboard() {
        TenantContext.set(7L);
        when(userRepository.findTopStudentsByXp(7L, 10)).thenReturn(List.of(
            new User(1L, 7L, "a@test.com", "HASH", "Alice", UserRole.STUDENT,
                Instant.now(), 40, 5, LocalDate.now(), 1, Set.of()),
            new User(2L, 7L, "b@test.com", "HASH", "Bob", UserRole.STUDENT,
                Instant.now(), 30, 3, LocalDate.now(), 0, Set.of())
        ));

        List<LeaderboardUserResponse> response = service.getLeaderboard();

        assertThat(response).hasSize(2);
        assertThat(response.get(0).fullName()).isEqualTo("Alice");
        assertThat(response.get(0).points()).isEqualTo(40);
        assertThat(response.get(0).streak()).isEqualTo(5);
        verify(userRepository).findTopStudentsByXp(7L, 10);
    }

    @Test
    void shouldThrowWhenGettingLeaderboardWithoutTenantContext() {
        assertThatThrownBy(() -> service.getLeaderboard())
            .isInstanceOf(TenantViolationException.class)
            .hasMessageContaining("No hay un institutionId");
    }
}
