package com.streakstudy.infrastructure.tenancy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import com.streakstudy.domain.exception.TenantViolationException;

@ExtendWith(MockitoExtension.class)
class TenantContextTest {

    @AfterEach
    void cleanup() {
        TenantContext.clear();
    }

    @Test
    void set_yGet_devuelvenElMismoValor() {
        TenantContext.set(42L);
        assertThat(TenantContext.getInstitutionId()).isEqualTo(42L);
        assertThat(TenantContext.hasTenant()).isTrue();
    }

    @Test
    void clear_eliminaElValor() {
        TenantContext.set(42L);
        TenantContext.clear();
        assertThat(TenantContext.getInstitutionId()).isNull();
        assertThat(TenantContext.hasTenant()).isFalse();
    }

    @Test
    void requireInstitutionId_lanzaCuandoNoHayContextoNiBypass() {
        assertThatThrownBy(TenantContext::requireInstitutionId)
            .isInstanceOf(TenantViolationException.class)
            .hasMessageContaining("No hay un institutionId");
    }

    @Test
    void set_rechazaNull() {
        assertThatThrownBy(() -> TenantContext.set(null))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void runCrossTenant_marcaElThreadComoCrossTenant() {
        assertThat(TenantContext.isCrossTenant()).isFalse();
        TenantContext.runCrossTenant(() -> {
            assertThat(TenantContext.isCrossTenant()).isTrue();
            assertThat(TenantContext.getInstitutionId()).isNull();
        });
        assertThat(TenantContext.isCrossTenant()).isFalse();
    }

    @Test
    void runCrossTenant_restauraElTenantPrevio() {
        TenantContext.set(7L);
        Long result = TenantContext.runCrossTenant(() -> {
            assertThat(TenantContext.getInstitutionId()).isNull();
            return 100L;
        });
        assertThat(result).isEqualTo(100L);
        assertThat(TenantContext.getInstitutionId()).isEqualTo(7L);
    }

    @Test
    void requireInstitutionId_enModoCrossTenantSinTenant_lanzaIllegalState() {
        TenantContext.runCrossTenant(() ->
            assertThatThrownBy(TenantContext::requireInstitutionId)
                .isInstanceOf(IllegalStateException.class));
    }
}
