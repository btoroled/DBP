package com.streakstudy.infrastructure.web;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.streakstudy.application.dto.LeaderboardUserResponse;
import com.streakstudy.application.service.LeaderboardService;
import com.streakstudy.infrastructure.security.JwtAuthenticationFilter;
import com.streakstudy.infrastructure.web.advice.GlobalExceptionHandler;

@WebMvcTest(LeaderboardController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class LeaderboardControllerTest {

    @Autowired MockMvc mockMvc;

    @MockitoBean LeaderboardService leaderboardService;
    @MockitoBean JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    void shouldReturnLeaderboardWhenGetCollection() throws Exception {
        when(leaderboardService.getLeaderboard()).thenReturn(List.of(
            new LeaderboardUserResponse(1L, "Alice", 7, 120),
            new LeaderboardUserResponse(2L, "Bob", 3, 80)
        ));

        mockMvc.perform(get("/api/v1/leaderboard"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].id").value(1L))
            .andExpect(jsonPath("$[0].fullName").value("Alice"))
            .andExpect(jsonPath("$[0].points").value(120))
            .andExpect(jsonPath("$[1].streak").value(3));
    }
}
