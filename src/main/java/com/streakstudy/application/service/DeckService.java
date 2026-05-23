package com.streakstudy.application.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.streakstudy.application.dto.CreateDeckRequest;
import com.streakstudy.application.dto.DeckResponse;
import com.streakstudy.application.dto.UpdateDeckRequest;
import com.streakstudy.domain.exception.EntityNotFoundException;
import com.streakstudy.domain.model.Deck;
import com.streakstudy.domain.repository.DeckRepository;
import com.streakstudy.infrastructure.tenancy.TenantContext;

@Service
public class DeckService {

    private final DeckRepository decks;

    public DeckService(DeckRepository decks) {
        this.decks = decks;
    }

    @Transactional
    public DeckResponse create(CreateDeckRequest req) {

        Long tenantId = TenantContext.requireInstitutionId();

        Deck toSave = Deck.newInstance(
                tenantId,
                req.name(),
                req.description()
        );

        return DeckResponse.from(
                decks.save(toSave)
        );
    }

    @Transactional(readOnly = true)
    public List<DeckResponse> listForCurrentTenant() {

        Long tenantId = TenantContext.requireInstitutionId();

        return decks.findAllByInstitutionId(tenantId)
                .stream()
                .map(DeckResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public DeckResponse getByIdForCurrentTenant(Long id) {

        Long tenantId = TenantContext.requireInstitutionId();

        return decks.findByIdAndInstitutionId(id, tenantId)
                .map(DeckResponse::from)
                .orElseThrow(() -> tenantScopedNotFound(id, tenantId));
    }

    @Transactional
    public DeckResponse updateForCurrentTenant(
            Long id,
            UpdateDeckRequest req
    ) {

        Long tenantId = TenantContext.requireInstitutionId();

        Deck existing = decks.findByIdAndInstitutionId(id, tenantId)
                .orElseThrow(() -> tenantScopedNotFound(id, tenantId));

        Deck updated = new Deck(
                existing.id(),
                existing.institutionId(),
                req.name(),
                req.description(),
                existing.createdAt()
        );

        return DeckResponse.from(
                decks.save(updated)
        );
    }

    @Transactional
    public void deleteByIdForCurrentTenant(Long id) {

        Long tenantId = TenantContext.requireInstitutionId();

        decks.findByIdAndInstitutionId(id, tenantId)
                .orElseThrow(() -> tenantScopedNotFound(id, tenantId));

        decks.deleteByIdAndInstitutionId(id, tenantId);
    }

    private RuntimeException tenantScopedNotFound(
            Long id,
            Long tenantId
    ) {
        return new EntityNotFoundException(
                "Deck",
                "%s (tenant=%s)".formatted(id, tenantId)
        );
    }
}