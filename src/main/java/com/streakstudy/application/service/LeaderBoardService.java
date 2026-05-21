package com.streakstudy.application.service;
import com.streakstudy.application.dto.LeaderBoardEntryDTo;
import com.streakstudy.domain.model.User;
import com.streakstudy.domain.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class LeaderBoardService {

    private final UserRepository userRepository;

    public LeaderBoardService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Obtiene el Top N de estudiantes de una institución con su ranking calculado.
     */
    @Transactional(readOnly = true)
    public List<LeaderBoardEntryDTo> getLeaderboard(Long institutionId, int topN) {
        List<User> topStudents = userRepository.findTopStudentsByXp(institutionId, topN);

        List<LeaderBoardEntryDTo> leaderboard = new ArrayList<>();
        int currentPosition = 1;

        for (User student : topStudents) {
            leaderboard.add(new LeaderBoardEntryDTo(
                    currentPosition,
                    student.fullName(),
                    student.xp()
            ));
            currentPosition++;
        }

        return leaderboard;
    }
}