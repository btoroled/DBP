package com.streakstudy.infrastructure.web;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.streakstudy.application.dto.RewardItemResponse;
import com.streakstudy.application.service.RewardItemService;

@RestController
@RequestMapping("/api/rewards")
public class RewardItemController {

    private final RewardItemService rewardItemService;

    public RewardItemController(RewardItemService rewardItemService) {
        this.rewardItemService = rewardItemService;
    }

    @GetMapping
    public ResponseEntity<List<RewardItemResponse>> getStoreCatalog() {
        List<RewardItemResponse> catalog = rewardItemService.getStoreCatalog();
        return ResponseEntity.ok(catalog);
    }
}