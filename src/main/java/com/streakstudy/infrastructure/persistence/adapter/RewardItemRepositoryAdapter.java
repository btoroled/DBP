package com.streakstudy.infrastructure.persistence.adapter;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.streakstudy.domain.model.RewardItem;
import com.streakstudy.domain.repository.RewardItemRepository;
import com.streakstudy.infrastructure.persistence.entity.RewardItemJpa;
import com.streakstudy.infrastructure.persistence.mapper.RewardItemMapper;
import com.streakstudy.infrastructure.persistence.repository.RewardItemJpaRepository;
import com.streakstudy.infrastructure.tenancy.TenantContext;

@Component
public class RewardItemRepositoryAdapter implements RewardItemRepository {

    private final RewardItemJpaRepository jpa;

    public RewardItemRepositoryAdapter(RewardItemJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public RewardItem save(RewardItem rewardItem) {
        return TenantContext.runCrossTenant(() -> {
            RewardItemJpa saved = jpa.save(RewardItemMapper.toJpa(rewardItem));
            return RewardItemMapper.toDomain(saved);
        });
    }

    @Override
    public Optional<RewardItem> findByIdAndInstitutionId(Long id, Long institutionId) {
        return jpa.findById(id)
                .filter(item -> item.getInstitutionId().equals(institutionId))
                .map(RewardItemMapper::toDomain);
    }

    @Override
    public List<RewardItem> findByInstitutionId(Long institutionId) {
        return jpa.findByInstitutionId(institutionId)
                .stream()
                .map(RewardItemMapper::toDomain)
                .collect(Collectors.toList());
    }
}