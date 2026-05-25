package com.streakstudy.infrastructure.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PasswordResetRateLimiterTest {

    @Test
    void shouldAllowUpToMaxAndRejectOverLimit() {
        PasswordResetRateLimiter limiter = new PasswordResetRateLimiter(3, 60);

        assertThat(limiter.tryAcquire("a@b.com")).isTrue();
        assertThat(limiter.tryAcquire("a@b.com")).isTrue();
        assertThat(limiter.tryAcquire("a@b.com")).isTrue();
        assertThat(limiter.tryAcquire("a@b.com")).isFalse();
    }

    @Test
    void shouldTreatDifferentEmailsAsSeparateBuckets() {
        PasswordResetRateLimiter limiter = new PasswordResetRateLimiter(1, 60);

        assertThat(limiter.tryAcquire("a@b.com")).isTrue();
        assertThat(limiter.tryAcquire("a@b.com")).isFalse();
        assertThat(limiter.tryAcquire("b@b.com")).isTrue();
    }

    @Test
    void shouldNormalizeEmailToLowercase() {
        PasswordResetRateLimiter limiter = new PasswordResetRateLimiter(1, 60);

        assertThat(limiter.tryAcquire("Alice@Utec.edu")).isTrue();
        assertThat(limiter.tryAcquire("alice@utec.edu")).isFalse();
    }
}
