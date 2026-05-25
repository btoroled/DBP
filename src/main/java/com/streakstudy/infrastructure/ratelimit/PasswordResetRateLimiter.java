package com.streakstudy.infrastructure.ratelimit;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Rate limiter in-memory para {@code POST /auth/password/forgot}.
 *
 * <p>Sliding window por email: si en la ventana han llegado {@code maxRequests}
 * o mas solicitudes, la siguiente es rechazada. No persiste — apto solo para
 * un solo nodo. En produccion con N replicas, sustituir por Redis o similar.</p>
 *
 * <p>Diseno: para cada email, mantenemos un deque de timestamps. En cada
 * intento purgamos los timestamps fuera de la ventana, y si el tamano del
 * deque es {@code >= maxRequests} rechazamos. Es sincronizado por simplicidad
 * — la carga esperada es baja.</p>
 */
@Component
public class PasswordResetRateLimiter {

    private final int maxRequests;
    private final Duration window;
    private final Map<String, Deque<Instant>> attempts = new HashMap<>();

    public PasswordResetRateLimiter(
            @Value("${app.password-reset.rate-limit.max-requests:5}") int maxRequests,
            @Value("${app.password-reset.rate-limit.window-minutes:60}") long windowMinutes) {
        this.maxRequests = maxRequests;
        this.window = Duration.ofMinutes(windowMinutes);
    }

    public synchronized boolean tryAcquire(String email) {
        if (email == null) {
            return true; // dejar pasar al service; ya valida el null y responde 202
        }
        String key = email.toLowerCase();
        Instant now = Instant.now();
        Instant cutoff = now.minus(window);

        Deque<Instant> queue = attempts.computeIfAbsent(key, k -> new ArrayDeque<>());
        while (!queue.isEmpty() && queue.peekFirst().isBefore(cutoff)) {
            queue.pollFirst();
        }

        if (queue.size() >= maxRequests) {
            return false;
        }
        queue.addLast(now);
        return true;
    }

    /** Solo para tests. */
    synchronized void reset() {
        attempts.clear();
    }
}
