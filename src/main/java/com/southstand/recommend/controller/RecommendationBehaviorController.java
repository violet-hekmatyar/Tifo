package com.southstand.recommend.controller;

import com.southstand.common.result.Result;
import com.southstand.recommend.dto.RecommendationBehaviorBatchRequest;
import com.southstand.recommend.service.RecommendationBehaviorService;
import com.southstand.recommend.vo.RecommendationBehaviorBatchResult;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/app/recommendation/behaviors")
public class RecommendationBehaviorController {

    private final RecommendationBehaviorService service;

    public RecommendationBehaviorController(RecommendationBehaviorService service) {
        this.service = service;
    }

    @PostMapping("/batch")
    public Result<RecommendationBehaviorBatchResult> batch(
            @Valid @RequestBody RecommendationBehaviorBatchRequest request) {
        return Result.success(service.saveBatch(request));
    }
}

