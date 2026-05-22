package com.streakstudy.infrastructure.web;

import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.streakstudy.application.dto.AuthResponse;
import com.streakstudy.application.dto.LoginRequest;
import com.streakstudy.application.dto.RegisterRequest;
import com.streakstudy.application.service.AuthService;
import com.streakstudy.domain.model.UserRole;
import com.streakstudy.infrastructure.security.JwtAuthenticationFilter;
import com.streakstudy.infrastructure.web.advice.GlobalExceptionHandler;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class AuthControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockitoBean AuthService authService;
    @MockitoBean JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    void shouldReturn201AndExpectedBodyWhenRegistering() throws Exception {
        RegisterRequest request = new RegisterRequest(1L, "alice@test.com", "Password123", "Alice");
        AuthResponse response = new AuthResponse("jwt-token", 3600L, 10L, 1L, "alice@test.com", UserRole.STUDENT, 0);
        when(authService.register(request)).thenReturn(response);

        mockMvc.perform(post("/api/auth/register")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.token").value("jwt-token"))
            .andExpect(jsonPath("$.institutionId").value(1L))
            .andExpect(jsonPath("$.role").value("STUDENT"));
    }

    @Test
    void shouldReturn200AndExpectedBodyWhenLoggingIn() throws Exception {
        LoginRequest request = new LoginRequest("alice@test.com", "Password123");
        AuthResponse response = new AuthResponse("jwt-token", 3600L, 10L, 1L, "alice@test.com", UserRole.STUDENT, 7);
        when(authService.login(request)).thenReturn(response);

        mockMvc.perform(post("/api/auth/login")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token").value("jwt-token"))
            .andExpect(jsonPath("$.xp").value(7));
    }

    @Test
    void shouldReturn400WhenRegisterBodyIsInvalid() throws Exception {
        RegisterRequest request = new RegisterRequest(null, "correo-invalido", "123", "");

        mockMvc.perform(post("/api/auth/register")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("validation_error"))
            .andExpect(jsonPath("$.errors").isArray());
    }
}
