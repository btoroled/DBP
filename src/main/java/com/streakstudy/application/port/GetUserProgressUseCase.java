package com.streakstudy.application.port;

import com.streakstudy.application.dto.UserProgressResponse;

public interface GetUserProgressUseCase {
    UserProgressResponse execute(Long userId);
}
