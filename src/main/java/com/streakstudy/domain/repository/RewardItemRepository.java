package com.streakstudy.domain.repository;

import java.util.List;
import java.util.Optional;
import com.streakstudy.domain.model.RewardItem;

/**
 * Puerto de persistencia para gestionar los articulos de la tienda de recompensas.
 */
public interface RewardItemRepository {

    RewardItem save(RewardItem rewardItem);

    Optional<RewardItem> findByIdAndInstitutionId(Long id, Long institutionId);

    // Devuelve todos los premios disponibles para el tenant actual
    List<RewardItem> findByInstitutionId(Long institutionId);
}