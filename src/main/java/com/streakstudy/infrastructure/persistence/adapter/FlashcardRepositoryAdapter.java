package com.streakstudy.infrastructure.persistence.adapter;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.streakstudy.domain.model.Flashcard;
import com.streakstudy.domain.repository.FlashcardRepository;
import com.streakstudy.infrastructure.persistence.mapper.FlashcardMapper;
import com.streakstudy.infrastructure.persistence.repository.FlashcardJpaRepository;

@Component
public class FlashcardRepositoryAdapter
        implements FlashcardRepository {

    private final FlashcardJpaRepository jpa;

    public FlashcardRepositoryAdapter(
            FlashcardJpaRepository jpa
    ) {
        this.jpa = jpa;
    }

    @Override
    public Flashcard save(Flashcard flashcard) {

        return FlashcardMapper.toDomain(
                jpa.save(
                        FlashcardMapper.toJpa(flashcard)
                )
        );
    }

    @Override
    public Optional<Flashcard> findByIdAndInstitutionId(
            Long id,
            Long institutionId
    ) {
        return jpa.findByIdAndInstitutionId(id, institutionId)
                .map(FlashcardMapper::toDomain);
    }

    @Override
    public List<Flashcard> findAllByDeckIdAndInstitutionId(
            Long deckId,
            Long institutionId
    ) {
        return jpa
                .findAllByDeckIdAndInstitutionId(
                        deckId,
                        institutionId
                )
                .stream()
                .map(FlashcardMapper::toDomain)
                .toList();
    }

    @Override
    public void deleteByIdAndInstitutionId(
            Long id,
            Long institutionId
    ) {
        jpa.deleteByIdAndInstitutionIdScoped(
                id,
                institutionId
        );
    }
}