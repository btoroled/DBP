package com.streakstudy.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.streakstudy.domain.model.User;
import com.streakstudy.domain.model.UserRole;
import com.streakstudy.domain.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class StreakResetServiceTest {

    @Mock UserRepository userRepository;

    @InjectMocks StreakResetService service;

    @Test
    void shouldConsumeFreezeWhenInactiveUserStillHasOne() {
        User protectedUser = user(10L, 1L, 40, 6, LocalDate.now().minusDays(3), 1);
        when(userRepository.findAllInactiveSince(any(LocalDate.class), eq(1L))).thenReturn(List.of(protectedUser));

        service.executeReset(1L);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();
        assertThat(saved.streakFreezes()).isZero();
        assertThat(saved.currentStreak()).isEqualTo(6);
    }

    @Test
    void shouldResetStreakWhenInactiveUserHasNoFreeze() {
        User unprotectedUser = user(20L, 1L, 25, 4, LocalDate.now().minusDays(5), 0);
        when(userRepository.findAllInactiveSince(any(LocalDate.class), eq(1L))).thenReturn(List.of(unprotectedUser));

        service.executeReset(1L);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();
        assertThat(saved.currentStreak()).isZero();
        assertThat(saved.streakFreezes()).isZero();
    }

    @Test
    void shouldProcessGlobalResetWhenInstitutionIdIsNotProvided() {
        when(userRepository.findAllInactiveSince(any(LocalDate.class), eq((Long) null))).thenReturn(List.of(
            user(1L, 1L, 10, 2, LocalDate.now().minusDays(4), 0),
            user(2L, 2L, 12, 3, LocalDate.now().minusDays(2), 1)
        ));

        service.executeReset();

        verify(userRepository).findAllInactiveSince(any(LocalDate.class), eq((Long) null));
        verify(userRepository, times(2)).save(any(User.class));
    }

    private User user(Long id, Long institutionId, int xp, int streak, LocalDate lastActiveDate, int freezes) {
        return new User(
            id,
            institutionId,
            "user" + id + "@test.com",
            "HASH",
            "User " + id,
            UserRole.STUDENT,
            Instant.now(),
            xp,
            streak,
            lastActiveDate,
            freezes,
            Set.of()
        );
    }
}
