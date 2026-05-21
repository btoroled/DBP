package com.streakstudy.infrastructure.web;

import com.streakstudy.application.dto.FinishReviewRequest;
import com.streakstudy.application.dto.UserProgressResponse;
import com.streakstudy.application.port.FinishReviewUseCase;
import com.streakstudy.application.port.GetUserProgressUseCase;
import com.streakstudy.infrastructure.security.AuthenticatedUserPrincipal;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users/me/progress")
public class UserProgressController {
    private final FinishReviewUseCase finishReviewUseCase;
    private final GetUserProgressUseCase getUserProgressUseCase;

    public UserProgressController(FinishReviewUseCase finishReviewUseCase,
                                  GetUserProgressUseCase getUserProgressUseCase) {
        this.finishReviewUseCase = finishReviewUseCase;
        this.getUserProgressUseCase = getUserProgressUseCase;
    }

    @PostMapping("/review")
    public ResponseEntity<Void> finishReview(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            @Valid @RequestBody FinishReviewRequest request
    ) {
        Long userId = principal.userId();
        finishReviewUseCase.execute(userId, request);
        return ResponseEntity.ok().build();
    }

    @GetMapping
    public ResponseEntity<UserProgressResponse> getMyProgress(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal
    ) {
        Long userId = principal.userId();

        UserProgressResponse response = getUserProgressUseCase.execute(userId);

        return ResponseEntity.ok(response);
    }

}
