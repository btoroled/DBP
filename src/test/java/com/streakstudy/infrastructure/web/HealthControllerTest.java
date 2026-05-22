package com.streakstudy.infrastructure.web;

import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * Test unitario del HealthController usando Mockito + MockMvc standalone.
 *
 * <p>No levanta el contexto completo de Spring: probamos solo la capa web,
 * sin tocar la base de datos. Asi el test es rapido y deterministico.</p>
 */
@ExtendWith(MockitoExtension.class)
class HealthControllerTest {

    private HealthController controller;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        // Construimos el controller con un spy de Mockito para poder verificar
        // que el metodo health() es invocado correctamente.
        controller = spy(new HealthController("StreakStudy API"));
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void shouldReturnUpStatusAndApplicationNameWhenCheckingHealth() throws Exception {
        mockMvc.perform(get("/api/health"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("UP"))
            .andExpect(jsonPath("$.application").value("StreakStudy API"))
            .andExpect(jsonPath("$.timestamp", notNullValue()));

        verify(controller, times(1)).health();
    }

    @Test
    void shouldUseInjectedApplicationNameWhenCheckingHealth() throws Exception {
        HealthController otroController = new HealthController("nombre-custom");
        MockMvc otroMockMvc = MockMvcBuilders.standaloneSetup(otroController).build();

        otroMockMvc.perform(get("/api/health"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.application").value("nombre-custom"));
    }
}
