package com.streakstudy.infrastructure.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.streakstudy.application.service.CourseService;
import com.streakstudy.infrastructure.security.JwtAuthenticationFilter;
import com.streakstudy.infrastructure.web.advice.GlobalExceptionHandler;

/**
 * Tests de autorizacion de {@link CourseController}. Habilitamos
 * {@code @EnableMethodSecurity} via un {@code @TestConfiguration} minimo
 * para que las anotaciones {@code @PreAuthorize} del controller se enforcen
 * (lo que NO ocurre en {@link CourseControllerTest} normal con
 * {@code @WebMvcTest} default).
 */
@WebMvcTest(CourseController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({GlobalExceptionHandler.class, CourseControllerAuthorizationTest.MethodSecurityTestConfig.class})
class CourseControllerAuthorizationTest {

    @EnableMethodSecurity
    static class MethodSecurityTestConfig { }

    @Autowired MockMvc mockMvc;

    @MockitoBean CourseService courseService;
    @MockitoBean JwtAuthenticationFilter jwtAuthenticationFilter;

    // ── POST /api/v1/courses ─ requiere TEACHER, INSTITUTION_ADMIN o SUPER_ADMIN ──

    @Test
    @WithAnonymousUser
    void shouldReturn403WhenAnonymousCreatesCourse() throws Exception {
        // Cuerpo valido para que la validacion no responda 400 antes de
        // que @PreAuthorize evalue. Con addFilters=false, AccessDenied
        // (anonymous) cae al @ExceptionHandler como 403 forbidden.
        mockMvc.perform(post("/api/v1/courses")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Calculo\",\"description\":\"x\"}"))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "STUDENT")
    void shouldReturn403WhenStudentCreatesCourse() throws Exception {
        mockMvc.perform(post("/api/v1/courses")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Calculo\",\"description\":\"x\"}"))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "TEACHER")
    void shouldReturn201WhenTeacherCreatesCourse() throws Exception {
        mockMvc.perform(post("/api/v1/courses")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Calculo\",\"description\":\"x\"}"))
            .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(authorities = "INSTITUTION_ADMIN")
    void shouldReturn201WhenAdminCreatesCourse() throws Exception {
        mockMvc.perform(post("/api/v1/courses")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Calculo\",\"description\":\"x\"}"))
            .andExpect(status().isCreated());
    }

    // ── DELETE /api/v1/courses/{id} ─ requiere INSTITUTION_ADMIN o SUPER_ADMIN ──

    @Test
    @WithMockUser(authorities = "STUDENT")
    void shouldReturn403WhenStudentDeletesCourse() throws Exception {
        mockMvc.perform(delete("/api/v1/courses/1"))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "TEACHER")
    void shouldReturn403WhenTeacherDeletesCourse() throws Exception {
        mockMvc.perform(delete("/api/v1/courses/1"))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "INSTITUTION_ADMIN")
    void shouldReturn204WhenAdminDeletesCourse() throws Exception {
        mockMvc.perform(delete("/api/v1/courses/1"))
            .andExpect(status().isNoContent());
    }

    // ── GET ─ cualquier autenticado ──

    @Test
    @WithMockUser(authorities = "STUDENT")
    void shouldReturn200WhenAuthenticatedUserListsCourses() throws Exception {
        mockMvc.perform(get("/api/v1/courses"))
            .andExpect(status().isOk());
    }
}
