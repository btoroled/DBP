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

import com.streakstudy.application.dto.RewardItemResponse;
import com.streakstudy.application.service.RewardItemService;
import com.streakstudy.infrastructure.security.JwtAuthenticationFilter;
import com.streakstudy.infrastructure.web.advice.GlobalExceptionHandler;

@WebMvcTest(RewardItemController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class RewardItemControllerTest {

    @Autowired MockMvc mockMvc;

    @MockitoBean RewardItemService rewardItemService;
    @MockitoBean JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    void shouldReturnRewardCatalogWhenRequested() throws Exception {
        when(rewardItemService.getStoreCatalog()).thenReturn(List.of(
            new RewardItemResponse(1L, "Cupon", "Descuento", 20, 3),
            new RewardItemResponse(2L, "Libre", "Dia libre", 50, 1)
        ));

        mockMvc.perform(get("/api/v1/rewards"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].title").value("Cupon"))
            .andExpect(jsonPath("$[1].costInPoints").value(50));
    }
}
