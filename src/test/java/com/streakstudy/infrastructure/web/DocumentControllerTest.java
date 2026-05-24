package com.streakstudy.infrastructure.web;

import com.streakstudy.application.dto.AiGenerationJobResponse;
import com.streakstudy.application.dto.DocumentStatusResponse;
import com.streakstudy.application.dto.DocumentUploadResponse;
import com.streakstudy.application.service.DocumentService;
import com.streakstudy.domain.model.AiGenerationJobStatus;
import com.streakstudy.domain.model.DocumentStatus;
import com.streakstudy.domain.model.UserRole;
import com.streakstudy.infrastructure.security.AuthenticatedUserPrincipal;
import com.streakstudy.infrastructure.security.JwtAuthenticationFilter;
import com.streakstudy.infrastructure.web.advice.GlobalExceptionHandler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DocumentController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class DocumentControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean DocumentService documentService;
    @MockitoBean JwtAuthenticationFilter jwtAuthenticationFilter;

    // Con addFilters=false el SecurityContextHolderFilter no corre, asi que el
    // post-processor .with(authentication(...)) no llega al ArgumentResolver de
    // @AuthenticationPrincipal. Poblamos el holder a mano para cada test.
    @BeforeEach
    void setUpSecurityContext() {
        SecurityContextHolder.getContext().setAuthentication(auth());
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private Authentication auth() {
        var principal = new AuthenticatedUserPrincipal(1L, 1L, "test@utec.edu.pe", UserRole.STUDENT);
        return new UsernamePasswordAuthenticationToken(principal, null,
                List.of(new SimpleGrantedAuthority("STUDENT")));
    }

    @Test
    void upload_archivoValido_retorna202ConDocumentId() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "apuntes.pdf", "application/pdf", new byte[100]);

        when(documentService.upload(any(), anyLong()))
                .thenReturn(new DocumentUploadResponse(1L, "apuntes.pdf", DocumentStatus.PENDING, false));

        mockMvc.perform(multipart("/api/v1/documents/upload")
                        .file(file))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.documentId").value(1))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.duplicate").value(false));
    }

    @Test
    void getStatus_documentoExistente_retorna200() throws Exception {
        when(documentService.getStatus(1L))
                .thenReturn(new DocumentStatusResponse(1L, "apuntes.pdf", DocumentStatus.READY, true));

        mockMvc.perform(get("/api/v1/documents/1/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("READY"))
                .andExpect(jsonPath("$.markdownAvailable").value(true));
    }

    @Test
    void getJobStatus_jobExistente_retornaEstadoYTokens() throws Exception {
        when(documentService.getJobStatus(5L))
                .thenReturn(new AiGenerationJobResponse(5L, 1L, 2L,
                        AiGenerationJobStatus.COMPLETED, 300, 150, 0.00084, null));

        mockMvc.perform(get("/api/v1/documents/jobs/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.totalInputTokens").value(300))
                .andExpect(jsonPath("$.estimatedCostUsd").value(0.00084));
    }
}
