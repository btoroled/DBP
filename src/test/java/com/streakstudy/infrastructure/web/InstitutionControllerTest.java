package com.streakstudy.infrastructure.web;

import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import com.streakstudy.application.dto.InstitutionRequest;
import com.streakstudy.application.dto.InstitutionResponse;
import com.streakstudy.application.dto.InstitutionSummaryResponse;
import com.streakstudy.application.service.InstitutionService;
import com.streakstudy.infrastructure.security.JwtAuthenticationFilter;
import com.streakstudy.infrastructure.web.advice.GlobalExceptionHandler;

@WebMvcTest(InstitutionController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class InstitutionControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockitoBean InstitutionService institutionService;
    @MockitoBean JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    void shouldCreateInstitutionWhenBodyIsValid() throws Exception {
        InstitutionRequest request = new InstitutionRequest("UTEC", "utec");
        InstitutionResponse response = new InstitutionResponse(1L, "UTEC", "utec", true, Instant.now());
        when(institutionService.create(request)).thenReturn(response);

        mockMvc.perform(post("/api/v1/institutions")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(1L))
            .andExpect(jsonPath("$.code").value("utec"));
    }

    @Test
    void shouldReturnInstitutionWhenIdExists() throws Exception {
        InstitutionResponse response = new InstitutionResponse(2L, "PUCP", "pucp", true, Instant.now());
        when(institutionService.getById(2L)).thenReturn(response);

        mockMvc.perform(get("/api/v1/institutions/2"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("PUCP"));
    }

    @Test
    void shouldReturnListOfActiveInstitutionsWhenGetCollection() throws Exception {
        when(institutionService.listActive()).thenReturn(List.of(
            new InstitutionSummaryResponse(1L, "PUCP"),
            new InstitutionSummaryResponse(2L, "UTEC")
        ));

        mockMvc.perform(get("/api/v1/institutions"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].id").value(1L))
            .andExpect(jsonPath("$[0].name").value("PUCP"))
            .andExpect(jsonPath("$[1].name").value("UTEC"));
    }

    @Test
    void shouldReturnBadRequestWhenCreateBodyIsInvalid() throws Exception {
        InstitutionRequest request = new InstitutionRequest("", "");

        mockMvc.perform(post("/api/v1/institutions")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("validation_error"));
    }
}
