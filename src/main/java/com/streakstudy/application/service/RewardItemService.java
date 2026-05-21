package com.streakstudy.application.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.streakstudy.application.dto.RewardItemResponse;
import com.streakstudy.domain.repository.RewardItemRepository;
import com.streakstudy.infrastructure.tenancy.TenantContext;

@Service
public class RewardItemService {

    private final RewardItemRepository rewardItemRepository;

    public RewardItemService(RewardItemRepository rewardItemRepository) {
        this.rewardItemRepository = rewardItemRepository;
    }

    @Transactional(readOnly = true)
    public List<RewardItemResponse> getStoreCatalog() {
        // Captura el tenant de la sesión actual de forma segura
        Long institutionId = TenantContext.requireInstitutionId();

        // Recupera de la base de datos y mapea al DTO de salida
        return rewardItemRepository.findByInstitutionId(institutionId)
                .stream()
                .map(item -> new RewardItemResponse(
                        item.id(),
                        item.title(),
                        item.description(),
                        item.costInPoints(),
                        item.stock()
                ))
                .collect(Collectors.toList());
    }
}