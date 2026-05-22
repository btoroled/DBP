package com.streakstudy.infrastructure.persistence.mapper;

import com.streakstudy.domain.model.RewardItem;
import com.streakstudy.infrastructure.persistence.entity.RewardItemJpa;

public final class RewardItemMapper {

    private RewardItemMapper() { }

    public static RewardItem toDomain(RewardItemJpa jpa) {
        return new RewardItem(
                jpa.getId(),
                jpa.getInstitutionId(),
                jpa.getTitle(),
                jpa.getDescription(),
                jpa.getCostInPoints(),
                jpa.getStock()
        );
    }

    public static RewardItemJpa toJpa(RewardItem domain) {
        RewardItemJpa jpa = new RewardItemJpa();
        jpa.setId(domain.id());
        jpa.setInstitutionId(domain.institutionId());
        jpa.setTitle(domain.title());
        jpa.setDescription(domain.description());
        jpa.setCostInPoints(domain.costInPoints());
        jpa.setStock(domain.stock());
        return jpa;
    }
}