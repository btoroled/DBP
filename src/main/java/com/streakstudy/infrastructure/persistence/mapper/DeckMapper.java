package com.streakstudy.infrastructure.persistence.mapper;

import com.streakstudy.domain.model.Deck;
import com.streakstudy.infrastructure.persistence.entity.DeckJpa;

public final class DeckMapper {

    private DeckMapper() {}

    public static Deck toDomain(DeckJpa jpa) {
        return new Deck(
                jpa.getId(),
                jpa.getInstitutionId(),
                jpa.getName(),
                jpa.getDescription(),
                jpa.getCreatedAt()
        );
    }

    public static DeckJpa toJpa(Deck domain) {
        DeckJpa jpa = new DeckJpa();

        jpa.setId(domain.id());
        jpa.setInstitutionId(domain.institutionId());
        jpa.setName(domain.name());
        jpa.setDescription(domain.description());
        jpa.setCreatedAt(domain.createdAt());

        return jpa;
    }
}