package com.streakstudy.infrastructure.web;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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
import com.streakstudy.application.dto.CourseResponse;
import com.streakstudy.application.dto.CreateCourseRequest;
import com.streakstudy.application.service.CourseService;
import com.streakstudy.infrastructure.security.JwtAuthenticationFilter;
import com.streakstudy.infrastructure.web.advice.GlobalExceptionHandler;

@WebMvcTest(CourseController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class CourseControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockitoBean CourseService courseService;
    @MockitoBean JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    void shouldReturn201AndCreatedCourseWhenCreating() throws Exception {
        CreateCourseRequest request = new CreateCourseRequest("Calculo", "desc");
        CourseResponse response = new CourseResponse(1L, 7L, "Calculo", "desc", Instant.now());
        when(courseService.create(request)).thenReturn(response);

        mockMvc.perform(post("/api/courses")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(1L))
            .andExpect(jsonPath("$.institutionId").value(7L))
            .andExpect(jsonPath("$.name").value("Calculo"));
    }

    @Test
    void shouldReturn200AndListWhenListingCourses() throws Exception {
        when(courseService.listForCurrentTenant()).thenReturn(List.of(
            new CourseResponse(1L, 7L, "Calculo", "desc", Instant.now()),
            new CourseResponse(2L, 7L, "Algebra", "desc", Instant.now())
        ));

        mockMvc.perform(get("/api/courses"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].name").value("Calculo"))
            .andExpect(jsonPath("$[1].name").value("Algebra"));
    }

    @Test
    void shouldReturn204WhenDeletingCourse() throws Exception {
        doNothing().when(courseService).deleteByIdForCurrentTenant(5L);

        mockMvc.perform(delete("/api/courses/5"))
            .andExpect(status().isNoContent());
    }

    @Test
    void shouldReturn400WhenCreateBodyIsInvalid() throws Exception {
        CreateCourseRequest request = new CreateCourseRequest("", "x".repeat(2100));

        mockMvc.perform(post("/api/courses")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("validation_error"));
    }
}
