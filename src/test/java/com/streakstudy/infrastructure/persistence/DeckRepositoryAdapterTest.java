package com.streakstudy.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import com.streakstudy.domain.model.Deck;
import com.streakstudy.domain.model.Institution;
import com.streakstudy.infrastructure.persistence.adapter.DeckRepositoryAdapter;
import com.streakstudy.infrastructure.persistence.adapter.InstitutionRepositoryAdapter;
import com.streakstudy.infrastructure.tenancy.TenantContext;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase
@Import({InstitutionRepositoryAdapter.class, DeckRepositoryAdapter.class})
class DeckRepositoryAdapterTest {

    @Autowired InstitutionRepositoryAdapter institutions;
    @Autowired DeckRepositoryAdapter decks;

    @AfterEach
    void cleanup() {
        TenantContext.clear();
    }

    @Test
    void shouldSaveAndAssignIdAndCreatedAtWhenInsertingDeck() {
        Long inst = institutions.save(Institution.newInstance("UTEC", "utec")).id();
        TenantContext.set(inst);

        Deck saved = decks.save(Deck.newInstance(inst, "Calculo", "desc"));

        assertThat(saved.id()).isNotNull();
        assertThat(saved.createdAt()).isNotNull();
        assertThat(saved.institutionId()).isEqualTo(inst);
        assertThat(saved.name()).isEqualTo("Calculo");
    }

    @Test
    void shouldFindAllByInstitutionIdScopedToTenantWhenListingDecks() {
        Long utec = institutions.save(Institution.newInstance("UTEC", "utec")).id();
        Long pucp = institutions.save(Institution.newInstance("PUCP", "pucp")).id();

        TenantContext.set(utec);
        decks.save(Deck.newInstance(utec, "Calc UTEC", ""));
        decks.save(Deck.newInstance(utec, "Alg UTEC", ""));

        TenantContext.set(pucp);
        decks.save(Deck.newInstance(pucp, "Calc PUCP", ""));

        TenantContext.set(utec);
        List<Deck> result = decks.findAllByInstitutionId(utec);

        assertThat(result).hasSize(2);
        assertThat(result).extracting(Deck::institutionId).containsOnly(utec);
    }

    @Test
    void shouldReturnEmptyWhenFindingDeckByIdAcrossTenants() {
        Long utec = institutions.save(Institution.newInstance("UTEC", "utec")).id();
        Long pucp = institutions.save(Institution.newInstance("PUCP", "pucp")).id();

        TenantContext.set(utec);
        Deck saved = decks.save(Deck.newInstance(utec, "Calc UTEC", ""));

        // Buscar el deck de UTEC desde el tenant PUCP debe devolver vacio
        Optional<Deck> crossTenant = decks.findByIdAndInstitutionId(saved.id(), pucp);
        Optional<Deck> sameTenant = decks.findByIdAndInstitutionId(saved.id(), utec);

        assertThat(crossTenant).isEmpty();
        assertThat(sameTenant).isPresent();
    }

    @Test
    void shouldCountByInstitutionIdWithinTenantOnly() {
        Long utec = institutions.save(Institution.newInstance("UTEC", "utec")).id();
        Long pucp = institutions.save(Institution.newInstance("PUCP", "pucp")).id();

        TenantContext.set(utec);
        decks.save(Deck.newInstance(utec, "A", ""));
        decks.save(Deck.newInstance(utec, "B", ""));

        TenantContext.set(pucp);
        decks.save(Deck.newInstance(pucp, "C", ""));

        assertThat(decks.countByInstitutionId(utec)).isEqualTo(2);
        assertThat(decks.countByInstitutionId(pucp)).isEqualTo(1);
    }

    @Test
    void shouldRemoveDeckOnlyForCorrectTenantWhenDeleting() {
        Long utec = institutions.save(Institution.newInstance("UTEC", "utec")).id();
        Long pucp = institutions.save(Institution.newInstance("PUCP", "pucp")).id();

        TenantContext.set(utec);
        Deck saved = decks.save(Deck.newInstance(utec, "Calc", ""));

        // Intentar borrar desde tenant equivocado: no debe afectar
        decks.deleteByIdAndInstitutionId(saved.id(), pucp);
        assertThat(decks.findByIdAndInstitutionId(saved.id(), utec)).isPresent();

        // Borrar desde el tenant correcto: ahora si
        decks.deleteByIdAndInstitutionId(saved.id(), utec);
        assertThat(decks.findByIdAndInstitutionId(saved.id(), utec)).isEmpty();
    }
}
