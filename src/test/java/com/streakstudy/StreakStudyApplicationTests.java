package com.streakstudy;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Smoke test del contexto de Spring: verifica que la aplicacion arranca
 * con perfil "test" (H2 + JWT de prueba), incluyendo seguridad, JPA y multi-tenancy.
 */
@SpringBootTest
@ActiveProfiles("test")
class StreakStudyApplicationTests {

    @Test
    void contextLoads() {
        // Si el contexto no carga, falla el test. Cubre arranque end-to-end.
    }
}
