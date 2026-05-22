package com.streakstudy.application.port;

import com.streakstudy.application.dto.FinishReviewRequest;

public interface FinishReviewUseCase {
    void execute(Long userId, FinishReviewRequest request);
}
