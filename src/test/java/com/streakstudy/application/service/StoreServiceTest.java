package com.streakstudy.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import com.streakstudy.domain.exception.BadgeAlreadyOwnedException;
import com.streakstudy.domain.exception.InsufficientXpException;
import com.streakstudy.domain.model.Badge;
import com.streakstudy.domain.model.User;
import com.streakstudy.domain.model.UserRole;
import com.streakstudy.domain.repository.UserRepository;

import jakarta.persistence.EntityNotFoundException;

@ExtendWith(MockitoExtension.class)
class StoreServiceTest {

    @Mock UserRepository userRepository;
    @Mock ApplicationEventPublisher events;
    @InjectMocks StoreService service;

    @Test
    void shouldDiscountXpAndIncreaseFreezeWhenBuyingStreakFreeze() {
        User user = new User(10L, 1L, "alice@test.com", "HASH", "Alice", UserRole.STUDENT,
            Instant.now(), 10, 3, LocalDate.now(), 0, Set.of());
        when(userRepository.findById(10L)).thenReturn(Optional.of(user));

        service.buyStreakFreeze(10L);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();
        assertThat(saved.xp()).isEqualTo(5);
        assertThat(saved.streakFreezes()).isEqualTo(1);
    }

    @Test
    void shouldSaveUpdatedUserWithBadgeWhenBuyingBadge() {
        User user = new User(10L, 1L, "alice@test.com", "HASH", "Alice", UserRole.STUDENT,
            Instant.now(), 20, 2, LocalDate.now(), 0, Set.of());
        when(userRepository.findById(10L)).thenReturn(Optional.of(user));

        service.buyBadge(10L, Badge.STREAK_STARTER);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();
        assertThat(saved.xp()).isEqualTo(13);
        assertThat(saved.badges()).contains(Badge.STREAK_STARTER);
    }

    @Test
    void shouldThrowWhenBuyingBadgeForMissingUser() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.buyBadge(99L, Badge.STREAK_STARTER))
            .isInstanceOf(EntityNotFoundException.class)
            .hasMessageContaining("Usuario no encontrado");

        verify(userRepository, never()).save(any());
    }

    @Test
    void shouldThrowWhenBuyingBadgeWithoutEnoughXp() {
        User user = new User(10L, 1L, "alice@test.com", "HASH", "Alice", UserRole.STUDENT,
            Instant.now(), 3, 1, LocalDate.now(), 0, Set.of());
        when(userRepository.findById(10L)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> service.buyBadge(10L, Badge.STREAK_STARTER))
            .isInstanceOf(InsufficientXpException.class);

        verify(userRepository, never()).save(any());
    }

    @Test
    void shouldThrowWhenBuyingBadgeAlreadyOwned() {
        User user = new User(10L, 1L, "alice@test.com", "HASH", "Alice", UserRole.STUDENT,
            Instant.now(), 20, 1, LocalDate.now(), 0, Set.of(Badge.STREAK_STARTER));
        when(userRepository.findById(10L)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> service.buyBadge(10L, Badge.STREAK_STARTER))
            .isInstanceOf(BadgeAlreadyOwnedException.class);

        verify(userRepository, never()).save(any());
    }
}
