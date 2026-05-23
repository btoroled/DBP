package com.streakstudy.infrastructure.persistence.adapter;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.streakstudy.domain.model.Deck;
import com.streakstudy.domain.repository.DeckRepository;
import com.streakstudy.infrastructure.persistence.entity.DeckJpa;
import com.streakstudy.infrastructure.persistence.mapper.DeckMapper;
import com.streakstudy.infrastructure.persistence.repository.DeckJpaRepository;

@Component
public class DeckRepositoryAdapter implements DeckRepository {

    private final DeckJpaRepository jpa;

    public DeckRepositoryAdapter(DeckJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Deck save(Deck deck) {
        DeckJpa saved = jpa.save(DeckMapper.toJpa(deck));
        return DeckMapper.toDomain(saved);
    }

    @Override
    public Optional<Deck> findByIdAndInstitutionId(
            Long id,
            Long institutionId
    ) {
        return jpa.findByIdAndInstitutionId(id, institutionId)
                .map(DeckMapper::toDomain);
    }

    @Override
    public List<Deck> findAllByInstitutionId(Long institutionId) {
        return jpa.findAllByInstitutionId(institutionId)
                .stream()
                .map(DeckMapper::toDomain)
                .toList();
    }

    @Override
    public long countByInstitutionId(Long institutionId) {
        return jpa.countByInstitutionId(institutionId);
    }

    @Override
    public void deleteByIdAndInstitutionId(
            Long id,
            Long institutionId
    ) {
        jpa.deleteByIdAndInstitutionIdScoped(id, institutionId);
    }
}