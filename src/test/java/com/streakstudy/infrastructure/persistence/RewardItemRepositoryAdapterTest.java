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

import com.streakstudy.domain.model.Institution;
import com.streakstudy.domain.model.RewardItem;
import com.streakstudy.infrastructure.persistence.adapter.InstitutionRepositoryAdapter;
import com.streakstudy.infrastructure.persistence.adapter.RewardItemRepositoryAdapter;
import com.streakstudy.infrastructure.tenancy.TenantContext;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase
@Import({InstitutionRepositoryAdapter.class, RewardItemRepositoryAdapter.class})
class RewardItemRepositoryAdapterTest {

    @Autowired InstitutionRepositoryAdapter institutions;
    @Autowired RewardItemRepositoryAdapter rewardItems;

    @AfterEach
    void cleanup() {
        TenantContext.clear();
    }

    @Test
    void shouldSaveAndListRewardItemsByTenant() {
        Long institutionId = institutions.save(Institution.newInstance("UTEC", "utec")).id();
        TenantContext.set(institutionId);

        RewardItem saved = rewardItems.save(new RewardItem(null, institutionId, "Cupon", "Descuento", 10, 4));
        List<RewardItem> listed = rewardItems.findByInstitutionId(institutionId);
        Optional<RewardItem> found = rewardItems.findByIdAndInstitutionId(saved.id(), institutionId);

        assertThat(saved.id()).isNotNull();
        assertThat(listed).hasSize(1);
        assertThat(found).isPresent();
        assertThat(found.get().title()).isEqualTo("Cupon");
    }
}
