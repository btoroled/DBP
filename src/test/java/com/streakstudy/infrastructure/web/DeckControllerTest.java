package com.streakstudy.infrastructure.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.streakstudy.application.dto.CreateDeckRequest;
import com.streakstudy.application.dto.DeckResponse;
import com.streakstudy.application.dto.UpdateDeckRequest;
import com.streakstudy.application.service.DeckService;
import com.streakstudy.domain.exception.EntityNotFoundException;
import com.streakstudy.infrastructure.security.JwtAuthenticationFilter;
import com.streakstudy.infrastructure.web.advice.GlobalExceptionHandler;

@WebMvcTest(DeckController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class DeckControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockitoBean DeckService deckService;
    @MockitoBean JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    void shouldReturn201AndCreatedDeckWhenCreating() throws Exception {
        CreateDeckRequest request = new CreateDeckRequest("Estructuras de Datos", "EDA básico");
        DeckResponse response = new DeckResponse(10L, 7L, "Estructuras de Datos", "EDA básico", Instant.now());
        when(deckService.create(any(CreateDeckRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/decks")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(10L))
            .andExpect(jsonPath("$.institutionId").value(7L))
            .andExpect(jsonPath("$.name").value("Estructuras de Datos"))
            .andExpect(jsonPath("$.description").value("EDA básico"));
    }

    @Test
    void shouldReturn200AndDeckListWhenListing() throws Exception {
        when(deckService.listForCurrentTenant()).thenReturn(List.of(
            new DeckResponse(1L, 7L, "Deck A", "desc A", Instant.now()),
            new DeckResponse(2L, 7L, "Deck B", "desc B", Instant.now())
        ));

        mockMvc.perform(get("/api/v1/decks"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value(1L))
            .andExpect(jsonPath("$[0].name").value("Deck A"))
            .andExpect(jsonPath("$[1].id").value(2L))
            .andExpect(jsonPath("$[1].name").value("Deck B"));
    }

    @Test
    void shouldReturn200AndDeckWhenFoundById() throws Exception {
        DeckResponse response = new DeckResponse(42L, 7L, "Algoritmos", "algo desc", Instant.now());
        when(deckService.getByIdForCurrentTenant(42L)).thenReturn(response);

        mockMvc.perform(get("/api/v1/decks/42"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(42L))
            .andExpect(jsonPath("$.name").value("Algoritmos"));
    }

    @Test
    void shouldReturn404WhenDeckNotFoundById() throws Exception {
        when(deckService.getByIdForCurrentTenant(999L))
            .thenThrow(new EntityNotFoundException("Deck", "999 (tenant=7)"));

        mockMvc.perform(get("/api/v1/decks/999"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error").value("not_found"))
            .andExpect(jsonPath("$.path").value("/api/v1/decks/999"));
    }

    @Test
    void shouldReturn200AndUpdatedDeckWhenUpdating() throws Exception {
        UpdateDeckRequest request = new UpdateDeckRequest("Nuevo Nombre", "Nueva descripción");
        DeckResponse response = new DeckResponse(5L, 7L, "Nuevo Nombre", "Nueva descripción", Instant.now());
        when(deckService.updateForCurrentTenant(eq(5L), any(UpdateDeckRequest.class))).thenReturn(response);

        mockMvc.perform(put("/api/v1/decks/5")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(5L))
            .andExpect(jsonPath("$.name").value("Nuevo Nombre"));
    }

    @Test
    void shouldReturn204WhenDeletingDeck() throws Exception {
        doNothing().when(deckService).deleteByIdForCurrentTenant(8L);

        mockMvc.perform(delete("/api/v1/decks/8"))
            .andExpect(status().isNoContent());
    }

    @Test
    void shouldReturn404WhenDeletingMissingDeck() throws Exception {
        doThrow(new EntityNotFoundException("Deck", "404 (tenant=7)"))
            .when(deckService).deleteByIdForCurrentTenant(404L);

        mockMvc.perform(delete("/api/v1/decks/404"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error").value("not_found"));
    }

    @Test
    void shouldReturn400WhenCreateBodyHasBlankName() throws Exception {
        CreateDeckRequest request = new CreateDeckRequest("", "desc");

        mockMvc.perform(post("/api/v1/decks")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("validation_error"));
    }

    @Test
    void shouldReturn400WhenCreateBodyHasNameTooLong() throws Exception {
        CreateDeckRequest request = new CreateDeckRequest("a".repeat(201), "desc");

        mockMvc.perform(post("/api/v1/decks")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("validation_error"));
    }

    @Test
    void shouldReturn400WhenUpdateBodyIsInvalid() throws Exception {
        UpdateDeckRequest request = new UpdateDeckRequest("", "desc");

        mockMvc.perform(put("/api/v1/decks/1")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("validation_error"));
    }
}
