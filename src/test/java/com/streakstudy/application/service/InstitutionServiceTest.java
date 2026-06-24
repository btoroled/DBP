package com.streakstudy.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.streakstudy.application.dto.InstitutionRequest;
import com.streakstudy.application.dto.InstitutionResponse;
import com.streakstudy.application.dto.InstitutionSummaryResponse;
import com.streakstudy.application.service.InstitutionService.InstitutionCodeAlreadyExistsException;
import com.streakstudy.domain.exception.EntityNotFoundException;
import com.streakstudy.domain.model.Institution;
import com.streakstudy.domain.repository.InstitutionRepository;

@ExtendWith(MockitoExtension.class)
class InstitutionServiceTest {

    @Mock InstitutionRepository institutions;
    @InjectMocks InstitutionService service;

    @Test
    void shouldPersistInstitutionAndReturnResponseWhenCreating() {
        InstitutionRequest req = new InstitutionRequest("UTEC", "UTEC");
        when(institutions.existsByCode("utec")).thenReturn(false);
        when(institutions.save(any(Institution.class))).thenAnswer(inv -> {
            Institution arg = inv.getArgument(0);
            return new Institution(99L, arg.name(), arg.code(), true, Instant.parse("2026-01-01T00:00:00Z"));
        });

        InstitutionResponse resp = service.create(req);

        assertThat(resp.id()).isEqualTo(99L);
        assertThat(resp.code()).isEqualTo("utec"); // normalizado a minusculas
        assertThat(resp.name()).isEqualTo("UTEC");
        assertThat(resp.active()).isTrue();

        ArgumentCaptor<Institution> captor = ArgumentCaptor.forClass(Institution.class);
        verify(institutions).save(captor.capture());
        assertThat(captor.getValue().code()).isEqualTo("utec");
    }

    @Test
    void shouldThrowWhenInstitutionCodeAlreadyExistsDuringCreate() {
        when(institutions.existsByCode("utec")).thenReturn(true);

        assertThatThrownBy(() -> service.create(new InstitutionRequest("UTEC", "utec")))
            .isInstanceOf(InstitutionCodeAlreadyExistsException.class);

        verify(institutions, never()).save(any());
    }

    @Test
    void shouldThrowWhenInstitutionDoesNotExistById() {
        when(institutions.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(404L))
            .isInstanceOf(EntityNotFoundException.class)
            .hasMessageContaining("Institution");
        verify(institutions, times(1)).findById(404L);
    }

    @Test
    void shouldReturnResponseWhenInstitutionExistsById() {
        Institution domain = new Institution(7L, "PUCP", "pucp", true, Instant.now());
        when(institutions.findById(7L)).thenReturn(Optional.of(domain));

        InstitutionResponse resp = service.getById(7L);
        assertThat(resp.id()).isEqualTo(7L);
        assertThat(resp.code()).isEqualTo("pucp");
    }

    @Test
    void shouldReturnOnlyIdAndNameWhenListingActiveInstitutions() {
        when(institutions.findAllActive()).thenReturn(List.of(
            new Institution(1L, "PUCP", "pucp", true, Instant.now()),
            new Institution(2L, "UTEC", "utec", true, Instant.now())
        ));

        List<InstitutionSummaryResponse> result = service.listActive();

        assertThat(result).hasSize(2);
        assertThat(result).extracting(InstitutionSummaryResponse::id).containsExactly(1L, 2L);
        assertThat(result).extracting(InstitutionSummaryResponse::name).containsExactly("PUCP", "UTEC");
    }
}
