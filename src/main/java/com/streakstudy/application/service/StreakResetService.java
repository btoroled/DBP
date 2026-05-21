package com.streakstudy.application.service;

import com.streakstudy.domain.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;

@Service
public class StreakResetService {

    private final UserRepository userRepository;

    public StreakResetService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional
    public void executeReset() {
        LocalDate yesterday = LocalDate.now().minusDays(1);

        int freezesUsed = userRepository.consumeStreakFreezes(yesterday);

        int streaksLost = userRepository.resetUnprotectedStreaks(yesterday);

        System.out.println("Cron Job nocturno finalizado:");
        System.out.println("Protectores de racha consumidos: " + freezesUsed);
        System.out.println("Rachas perdidas por inactividad: " + streaksLost);
    }
}