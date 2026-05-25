package com.streakstudy.infrastructure.web.advice;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.streakstudy.application.service.AuthService;
import com.streakstudy.application.service.PasswordResetService;
import com.streakstudy.infrastructure.ratelimit.PasswordResetRateLimiter;
import com.streakstudy.infrastructure.security.JwtAuthenticationFilter;
import com.streakstudy.infrastructure.web.AuthController;

/**
 * Cubre los handlers nuevos del Issue #4: {@code malformed_json} y
 * verificacion de que el body incluya el campo {@code path}.
 */
@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class GlobalExceptionHandlerTest {

    @Autowired MockMvc mockMvc;

    @MockitoBean AuthService authService;
    @MockitoBean PasswordResetService passwordResetService;
    @MockitoBean PasswordResetRateLimiter passwordResetRateLimiter;
    @MockitoBean JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    void shouldReturn400WithPathWhenJsonIsMalformed() throws Exception {
        // JSON truncado: el parser de Jackson lanza HttpMessageNotReadableException
        String malformed = "{\"email\": ";

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(APPLICATION_JSON)
                .content(malformed))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.error").value("malformed_json"))
            .andExpect(jsonPath("$.message").exists())
            .andExpect(jsonPath("$.timestamp").exists())
            .andExpect(jsonPath("$.path").value("/api/v1/auth/register"));
    }

    @Test
    void shouldIncludePathInValidationErrorResponse() throws Exception {
        // Body sintacticamente valido pero con campos invalidos -> validation_error
        String invalid = "{\"institutionId\": null, \"email\": \"x\", \"password\": \"\", \"fullName\": \"\"}";

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(APPLICATION_JSON)
                .content(invalid))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("validation_error"))
            .andExpect(jsonPath("$.path").value("/api/v1/auth/register"));
    }
}
