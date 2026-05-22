package com.streakstudy.infrastructure.job;
import com.streakstudy.application.service.StreakResetService;
import com.streakstudy.domain.repository.UserRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;

@Component
public class StreakResetJob {

    private final StreakResetService streakResetService;

    public StreakResetJob(StreakResetService streakResetService) {
        this.streakResetService = streakResetService;
    }

    @Scheduled(cron = "0 0 0 * * ?")
    @Transactional
    public void run() {
        streakResetService.executeReset();
    }
}