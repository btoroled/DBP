package com.streakstudy.application.service;

import com.streakstudy.domain.model.User;
import com.streakstudy.domain.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.List;

@Service
public class StreakResetService {

    private final UserRepository userRepository;

    public StreakResetService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    //se ejecuta de forma global, usuarios inactivos de todas las instituciones
    @Transactional
    public void executeReset() {
        LocalDate yesterday = LocalDate.now().minusDays(1);

        List<User> inactiveUsers = userRepository.findAllInactiveSince(yesterday, null);

        processReset(inactiveUsers);
    }

    // pruebas postman
    @Transactional
    public void executeReset(Long institutionId) {
        LocalDate yesterday = LocalDate.now().minusDays(1);

        List<User> inactiveUsers = userRepository.findAllInactiveSince(yesterday, institutionId);

        processReset(inactiveUsers);
    }

    private void processReset(List<User> inactiveUsers) {
        int freezesUsed = 0;
        int streaksLost = 0;

        for (User user : inactiveUsers) {
            if (user.streakFreezes() > 0) {
                User protectedUser = user.useStreakFreeze();
                userRepository.save(protectedUser);
                freezesUsed++;
            } else {
                User ruinedUser = user.resetStreakToZero();
                userRepository.save(ruinedUser);
                streaksLost++;
            }
        }

        System.out.println("Log de Cron Job");
        System.out.println("Usuarios evaluados: " + inactiveUsers.size());
        System.out.println("Protectores consumidos: " + freezesUsed);
        System.out.println("Rachas reiniciadas: " + streaksLost);
    }
}