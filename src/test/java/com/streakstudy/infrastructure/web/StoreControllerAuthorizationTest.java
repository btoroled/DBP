package com.streakstudy.infrastructure.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.streakstudy.application.service.StoreService;
import com.streakstudy.infrastructure.security.JwtAuthenticationFilter;
import com.streakstudy.infrastructure.web.advice.GlobalExceptionHandler;

/**
 * Tests de autorizacion para {@link StoreController}: ambas operaciones
 * (streak-freeze y badges) son exclusivas de STUDENT.
 */
@WebMvcTest(StoreController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({GlobalExceptionHandler.class, StoreControllerAuthorizationTest.MethodSecurityTestConfig.class})
class StoreControllerAuthorizationTest {

    @EnableMethodSecurity
    static class MethodSecurityTestConfig { }

    @Autowired MockMvc mockMvc;

    @MockitoBean StoreService storeService;
    @MockitoBean JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    @WithMockUser(authorities = "TEACHER")
    void shouldReturn403WhenTeacherBuysStreakFreeze() throws Exception {
        mockMvc.perform(post("/api/v1/store/streak-freeze"))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "INSTITUTION_ADMIN")
    void shouldReturn403WhenAdminBuysStreakFreeze() throws Exception {
        mockMvc.perform(post("/api/v1/store/streak-freeze"))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "TEACHER")
    void shouldReturn403WhenTeacherBuysBadge() throws Exception {
        mockMvc.perform(post("/api/v1/store/badges")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"badgeName\":\"STREAK_STARTER\"}"))
            .andExpect(status().isForbidden());
    }
}
