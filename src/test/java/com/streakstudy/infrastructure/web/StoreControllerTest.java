package com.streakstudy.infrastructure.web;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.streakstudy.application.service.StoreService;
import com.streakstudy.domain.exception.BadgeAlreadyOwnedException;
import com.streakstudy.domain.exception.InsufficientXpException;
import com.streakstudy.domain.exception.MaxStreakFreezesReachedException;
import com.streakstudy.domain.model.Badge;
import com.streakstudy.domain.model.UserRole;
import com.streakstudy.infrastructure.security.AuthenticatedUserPrincipal;
import com.streakstudy.infrastructure.web.advice.GlobalExceptionHandler;

@ExtendWith(MockitoExtension.class)
class StoreControllerTest {

    @Mock StoreService storeService;
    @InjectMocks StoreController controller;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().findAndRegisterModules();

        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
            .setControllerAdvice(new GlobalExceptionHandler())
            .setCustomArgumentResolvers(new FixedAuthenticatedUserPrincipalResolver())
            .setValidator(validator)
            .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
            .build();
    }

    @Test
    void shouldReturn200WhenBuyingStreakFreezeSucceeds() throws Exception {
        doNothing().when(storeService).buyStreakFreeze(10L);

        mockMvc.perform(post("/api/store/streak-freeze"))
            .andExpect(status().isOk());

        verify(storeService).buyStreakFreeze(10L);
    }

    @Test
    void shouldReturn400WhenBuyingStreakFreezeWithoutEnoughXp() throws Exception {
        doThrow(new InsufficientXpException()).when(storeService).buyStreakFreeze(10L);

        mockMvc.perform(post("/api/store/streak-freeze"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("insufficient_xp"));
    }

    @Test
    void shouldReturn400WhenMaxStreakFreezesReached() throws Exception {
        doThrow(new MaxStreakFreezesReachedException()).when(storeService).buyStreakFreeze(10L);

        mockMvc.perform(post("/api/store/streak-freeze"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("max_streak_freezes_reached"));
    }

    @Test
    void shouldReturn200WhenBuyingBadgeSucceeds() throws Exception {
        doNothing().when(storeService).buyBadge(10L, Badge.STREAK_STARTER);

        mockMvc.perform(post("/api/store/badges")
                .contentType(APPLICATION_JSON)
                .content("{\"badgeName\":\"STREAK_STARTER\"}"))
            .andExpect(status().isOk());

        verify(storeService).buyBadge(10L, Badge.STREAK_STARTER);
    }

    @Test
    void shouldReturn400WhenBadgeBodyIsInvalid() throws Exception {
        mockMvc.perform(post("/api/store/badges")
                .contentType(APPLICATION_JSON)
                .content("{\"badgeName\":null}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("validation_error"));
    }

    @Test
    void shouldReturn400WhenBuyingBadgeWithoutEnoughXp() throws Exception {
        doThrow(new InsufficientXpException()).when(storeService).buyBadge(10L, Badge.STREAK_STARTER);

        mockMvc.perform(post("/api/store/badges")
                .contentType(APPLICATION_JSON)
                .content("{\"badgeName\":\"STREAK_STARTER\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("insufficient_xp"));
    }

    @Test
    void shouldReturn409WhenBadgeAlreadyOwned() throws Exception {
        doThrow(new BadgeAlreadyOwnedException("STREAK_STARTER"))
            .when(storeService).buyBadge(10L, Badge.STREAK_STARTER);

        mockMvc.perform(post("/api/store/badges")
                .contentType(APPLICATION_JSON)
                .content("{\"badgeName\":\"STREAK_STARTER\"}"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.error").value("badge_already_owned"));
    }

    private static final class FixedAuthenticatedUserPrincipalResolver implements HandlerMethodArgumentResolver {
        @Override
        public boolean supportsParameter(MethodParameter parameter) {
            return parameter.getParameterType().equals(AuthenticatedUserPrincipal.class);
        }

        @Override
        public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                      NativeWebRequest webRequest, org.springframework.web.bind.support.WebDataBinderFactory binderFactory) {
            return new AuthenticatedUserPrincipal(10L, 7L, "alice@test.com", UserRole.STUDENT);
        }
    }
}
