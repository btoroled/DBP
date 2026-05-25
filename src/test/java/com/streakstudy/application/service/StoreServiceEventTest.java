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

import com.streakstudy.application.event.BadgeEarnedEvent;
import com.streakstudy.domain.exception.InsufficientXpException;
import com.streakstudy.domain.model.Badge;
import com.streakstudy.domain.model.User;
import com.streakstudy.domain.model.UserRole;
import com.streakstudy.domain.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class StoreServiceEventTest {

    @Mock UserRepository userRepository;
    @Mock ApplicationEventPublisher events;
    @InjectMocks StoreService service;

    @Test
    void shouldPublishBadgeEarnedEventWhenBuyBadgeSucceeds() {
        User user = new User(10L, 1L, "alice@utec.edu", "HASH", "Alice Perez", UserRole.STUDENT,
            Instant.now(), 20, 2, LocalDate.now(), 0, Set.of());
        when(userRepository.findById(10L)).thenReturn(Optional.of(user));

        service.buyBadge(10L, Badge.STREAK_STARTER);

        ArgumentCaptor<BadgeEarnedEvent> captor = ArgumentCaptor.forClass(BadgeEarnedEvent.class);
        verify(events).publishEvent(captor.capture());

        BadgeEarnedEvent ev = captor.getValue();
        assertThat(ev.userId()).isEqualTo(10L);
        assertThat(ev.email()).isEqualTo("alice@utec.edu");
        assertThat(ev.badgeName()).isEqualTo("STREAK_STARTER");
        assertThat(ev.badgeDisplayName()).isEqualTo(Badge.STREAK_STARTER.displayName());
    }

    @Test
    void shouldNotPublishEventWhenInsufficientXpToBuyBadge() {
        User user = new User(10L, 1L, "alice@utec.edu", "HASH", "Alice", UserRole.STUDENT,
            Instant.now(), 3, 1, LocalDate.now(), 0, Set.of());
        when(userRepository.findById(10L)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> service.buyBadge(10L, Badge.STREAK_STARTER))
            .isInstanceOf(InsufficientXpException.class);

        verify(events, never()).publishEvent(any());
    }
}
