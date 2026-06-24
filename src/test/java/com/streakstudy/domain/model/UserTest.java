package com.streakstudy.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Set;

import org.junit.jupiter.api.Test;

class UserTest {

    @Test
    void shouldPreserveBadgesAndStreakFreezesWhenIncrementingStreak() {
        User user = new User(
            1L, 1L, "alice@utec.edu", "HASH", "Alice", UserRole.STUDENT,
            Instant.now(), 20, 3, LocalDate.now().minusDays(1),
            2, Set.of(Badge.STREAK_STARTER)
        );

        User updated = user.incrementStreak(LocalDate.now());

        assertThat(updated.currentStreak()).isEqualTo(4);
        assertThat(updated.streakFreezes()).isEqualTo(2);
        assertThat(updated.badges()).containsExactly(Badge.STREAK_STARTER);
    }

    @Test
    void shouldResetStreakToOneAndKeepBadgesAndFreezesWhenGapIsLargerThanOneDay() {
        User user = new User(
            1L, 1L, "alice@utec.edu", "HASH", "Alice", UserRole.STUDENT,
            Instant.now(), 20, 7, LocalDate.now().minusDays(5),
            1, Set.of(Badge.STREAK_STARTER)
        );

        User updated = user.incrementStreak(LocalDate.now());

        assertThat(updated.currentStreak()).isEqualTo(1);
        assertThat(updated.streakFreezes()).isEqualTo(1);
        assertThat(updated.badges()).containsExactly(Badge.STREAK_STARTER);
    }

    @Test
    void shouldReturnSameInstanceWhenIncrementingStreakOnSameDay() {
        LocalDate today = LocalDate.now();
        User user = new User(
            1L, 1L, "alice@utec.edu", "HASH", "Alice", UserRole.STUDENT,
            Instant.now(), 20, 3, today, 2, Set.of(Badge.STREAK_STARTER)
        );

        User updated = user.incrementStreak(today);

        assertThat(updated).isSameAs(user);
    }
}
