package com.streakstudy.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.streakstudy.application.dto.CreateDeckRequest;
import com.streakstudy.application.dto.DeckResponse;
import com.streakstudy.application.dto.UpdateDeckRequest;
import com.streakstudy.domain.exception.EntityNotFoundException;
import com.streakstudy.domain.exception.TenantViolationException;
import com.streakstudy.domain.model.Deck;
import com.streakstudy.domain.repository.DeckRepository;
import com.streakstudy.infrastructure.tenancy.TenantContext;

@ExtendWith(MockitoExtension.class)
class DeckServiceTest {

    @Mock DeckRepository repo;
    @InjectMocks DeckService service;

    @AfterEach
    void cleanup() {
        TenantContext.clear();
    }

    @Test
    void shouldUseInstitutionIdFromTenantContextWhenCreatingDeck() {
        TenantContext.set(42L);
        when(repo.save(any(Deck.class))).thenAnswer(inv -> {
            Deck d = inv.getArgument(0);
            return new Deck(7L, d.institutionId(), d.name(), d.description(), Instant.now());
        });

        DeckResponse resp = service.create(new CreateDeckRequest("Calculo I", "desc"));

        assertThat(resp.id()).isEqualTo(7L);
        assertThat(resp.institutionId()).isEqualTo(42L);
        assertThat(resp.name()).isEqualTo("Calculo I");

        ArgumentCaptor<Deck> captor = ArgumentCaptor.forClass(Deck.class);
        verify(repo).save(captor.capture());
        assertThat(captor.getValue().institutionId()).isEqualTo(42L);
    }

    @Test
    void shouldThrowTenantViolationWhenCreatingDeckWithoutTenantContext() {
        assertThatThrownBy(() -> service.create(new CreateDeckRequest("Calculo", "")))
            .isInstanceOf(TenantViolationException.class);
        verify(repo, never()).save(any());
    }

    @Test
    void shouldFilterDecksByTenantWhenListingCurrentTenant() {
        TenantContext.set(7L);
        when(repo.findAllByInstitutionId(7L)).thenReturn(List.of(
            new Deck(1L, 7L, "A", "", Instant.now()),
            new Deck(2L, 7L, "B", "", Instant.now())
        ));

        List<DeckResponse> result = service.listForCurrentTenant();

        assertThat(result).hasSize(2);
        assertThat(result).allSatisfy(d -> assertThat(d.institutionId()).isEqualTo(7L));
        verify(repo).findAllByInstitutionId(7L);
    }

    @Test
    void shouldReturnDeckFromCurrentTenantWhenIdExists() {
        TenantContext.set(7L);
        Deck owned = new Deck(5L, 7L, "Deck", "", Instant.now());
        when(repo.findByIdAndInstitutionId(5L, 7L)).thenReturn(Optional.of(owned));

        DeckResponse resp = service.getByIdForCurrentTenant(5L);

        assertThat(resp.id()).isEqualTo(5L);
        assertThat(resp.institutionId()).isEqualTo(7L);
    }

    @Test
    void shouldThrowNotFoundWhenDeckIsMissingInCurrentTenant() {
        TenantContext.set(7L);
        when(repo.findByIdAndInstitutionId(99L, 7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getByIdForCurrentTenant(99L))
            .isInstanceOf(EntityNotFoundException.class)
            .hasMessageContaining("Deck")
            .hasMessageContaining("tenant=7");
    }

    @Test
    void shouldUpdateDeckPreservingIdAndInstitutionWhenItExists() {
        TenantContext.set(7L);
        Deck existing = new Deck(5L, 7L, "Viejo", "old desc", Instant.parse("2025-01-01T00:00:00Z"));
        when(repo.findByIdAndInstitutionId(5L, 7L)).thenReturn(Optional.of(existing));
        when(repo.save(any(Deck.class))).thenAnswer(inv -> inv.getArgument(0));

        DeckResponse resp = service.updateForCurrentTenant(5L, new UpdateDeckRequest("Nuevo", "new desc"));

        assertThat(resp.id()).isEqualTo(5L);
        assertThat(resp.institutionId()).isEqualTo(7L);
        assertThat(resp.name()).isEqualTo("Nuevo");
        assertThat(resp.description()).isEqualTo("new desc");

        ArgumentCaptor<Deck> captor = ArgumentCaptor.forClass(Deck.class);
        verify(repo).save(captor.capture());
        // createdAt no debe regenerarse
        assertThat(captor.getValue().createdAt()).isEqualTo(Instant.parse("2025-01-01T00:00:00Z"));
    }

    @Test
    void shouldThrowNotFoundWhenUpdatingMissingDeck() {
        TenantContext.set(7L);
        when(repo.findByIdAndInstitutionId(99L, 7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateForCurrentTenant(99L, new UpdateDeckRequest("X", "")))
            .isInstanceOf(EntityNotFoundException.class)
            .hasMessageContaining("Deck");
        verify(repo, never()).save(any());
    }

    @Test
    void shouldDeleteDeckOnlyWithinCurrentTenant() {
        TenantContext.set(7L);
        when(repo.findByIdAndInstitutionId(5L, 7L)).thenReturn(
            Optional.of(new Deck(5L, 7L, "D", "", Instant.now())));

        service.deleteByIdForCurrentTenant(5L);

        verify(repo).deleteByIdAndInstitutionId(5L, 7L);
    }

    @Test
    void shouldThrowNotFoundWhenDeletingMissingDeck() {
        TenantContext.set(7L);
        when(repo.findByIdAndInstitutionId(99L, 7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deleteByIdForCurrentTenant(99L))
            .isInstanceOf(EntityNotFoundException.class);
        verify(repo, never()).deleteByIdAndInstitutionId(any(), any());
    }
}
