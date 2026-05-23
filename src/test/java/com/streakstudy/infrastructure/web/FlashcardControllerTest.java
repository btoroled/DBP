package com.streakstudy.infrastructure.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.streakstudy.application.dto.CreateFlashcardRequest;
import com.streakstudy.application.service.FlashcardService;
import com.streakstudy.domain.model.Difficulty;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class FlashcardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private FlashcardService flashcardService;

    @Test
    @WithMockUser(username = "alumno@utec.edu.pe", roles = {"STUDENT"})
    void shouldCreateFlashcardSuccessfully() throws Exception {
        // Preparar el request DTO usando la dificultad correcta
        CreateFlashcardRequest request = new CreateFlashcardRequest(
                1L, // deckId
                "¿Qué es la inyección de dependencias?", // question
                "Un patrón de diseño de software...", // answer
                Difficulty.MEDIUM // enum asignado
        );

        mockMvc.perform(post("/api/v1/decks/1/flashcards")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }
}