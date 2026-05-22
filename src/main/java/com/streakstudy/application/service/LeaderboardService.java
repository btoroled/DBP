package com.streakstudy.application.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.streakstudy.application.dto.LeaderboardUserResponse;
import com.streakstudy.domain.repository.UserRepository;
import com.streakstudy.infrastructure.tenancy.TenantContext;

@Service
public class LeaderboardService {

    private final UserRepository userRepository;

    public LeaderboardService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<LeaderboardUserResponse> getLeaderboard() {
        // Captura el institutionId de la sesión del usuario actual de forma segura
        Long institutionId = TenantContext.requireInstitutionId();

        // Ejecuta la consulta ordenada que programamos en el adapter
        return userRepository.findLeaderboardByInstitutionId(institutionId)
                .stream()
                .map(user -> new LeaderboardUserResponse(
                        user.id(),
                        user.fullName(),
                        user.streak(),
                        user.points()
                ))
                .collect(Collectors.toList());
    }
}