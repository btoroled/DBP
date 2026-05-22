package com.streakstudy.infrastructure.web;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Set;

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
import com.streakstudy.application.dto.FinishReviewRequest;
import com.streakstudy.application.dto.UserProgressResponse;
import com.streakstudy.application.port.FinishReviewUseCase;
import com.streakstudy.application.port.GetUserProgressUseCase;
import com.streakstudy.domain.model.UserRole;
import com.streakstudy.infrastructure.security.AuthenticatedUserPrincipal;
import com.streakstudy.infrastructure.web.advice.GlobalExceptionHandler;

@ExtendWith(MockitoExtension.class)
class UserProgressControllerTest {

    @Mock FinishReviewUseCase finishReviewUseCase;
    @Mock GetUserProgressUseCase getUserProgressUseCase;
    @InjectMocks UserProgressController controller;

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
    void finishReview_conBodyValido_devuelve200() throws Exception {
        FinishReviewRequest request = new FinishReviewRequest(12, 20);
        doNothing().when(finishReviewUseCase).execute(10L, request);

        mockMvc.perform(post("/api/users/me/progress/review")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk());

        verify(finishReviewUseCase).execute(10L, request);
    }

    @Test
    void finishReview_conBodyInvalido_devuelve400() throws Exception {
        FinishReviewRequest request = new FinishReviewRequest(0, -1);

        mockMvc.perform(post("/api/users/me/progress/review")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("validation_error"));
    }

    @Test
    void getMyProgress_devuelve200YElBodyEsperado() throws Exception {
        when(getUserProgressUseCase.execute(10L))
            .thenReturn(new UserProgressResponse(14, 3, 1, Set.of("STREAK_STARTER")));

        mockMvc.perform(get("/api/users/me/progress"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.xp").value(14))
            .andExpect(jsonPath("$.currentStreak").value(3))
            .andExpect(jsonPath("$.badges[0]").value("STREAK_STARTER"));

        verify(getUserProgressUseCase).execute(10L);
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
