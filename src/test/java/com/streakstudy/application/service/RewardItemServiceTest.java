package com.streakstudy.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.streakstudy.application.dto.RewardItemResponse;
import com.streakstudy.domain.exception.TenantViolationException;
import com.streakstudy.domain.model.RewardItem;
import com.streakstudy.domain.repository.RewardItemRepository;
import com.streakstudy.infrastructure.tenancy.TenantContext;

@ExtendWith(MockitoExtension.class)
class RewardItemServiceTest {

    @Mock RewardItemRepository rewardItemRepository;

    @InjectMocks RewardItemService service;

    @AfterEach
    void cleanup() {
        TenantContext.clear();
    }

    @Test
    void shouldReturnTenantCatalogWhenTenantContextExists() {
        TenantContext.set(7L);
        when(rewardItemRepository.findByInstitutionId(7L)).thenReturn(List.of(
            new RewardItem(1L, 7L, "Cupon", "Descuento cafetería", 15, 5),
            new RewardItem(2L, 7L, "Sticker", "Pack premium", 5, 10)
        ));

        List<RewardItemResponse> catalog = service.getStoreCatalog();

        assertThat(catalog).hasSize(2);
        assertThat(catalog).extracting(RewardItemResponse::title)
            .containsExactly("Cupon", "Sticker");
        verify(rewardItemRepository).findByInstitutionId(7L);
    }

    @Test
    void shouldThrowWhenTenantContextIsMissing() {
        assertThatThrownBy(() -> service.getStoreCatalog())
            .isInstanceOf(TenantViolationException.class)
            .hasMessageContaining("No hay un institutionId");
    }
}
