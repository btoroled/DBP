package com.streakstudy.infrastructure.web;

import java.time.Instant;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoint de salud propio del API (independiente de Actuator).
 *
 * <p>Lo usamos como prueba de humo de SETUP-001: si {@code GET /api/health}
 * responde 200 OK con el JSON esperado, el proyecto levanto correctamente
 * y la capa web esta funcional.</p>
 */
@RestController
@RequestMapping("/api/health")
public class HealthController {

    private final String applicationName;

    public HealthController(@Value("${spring.application.name:streakstudy}") String applicationName) {
        this.applicationName = applicationName;
    }

    @GetMapping
    public Map<String, Object> health() {
        return Map.of(
            "status", "UP",
            "application", applicationName,
            "timestamp", Instant.now().toString()
        );
    }
}
