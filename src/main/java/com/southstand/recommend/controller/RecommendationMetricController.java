package com.southstand.recommend.controller;

import com.southstand.common.result.Result;
import com.southstand.recommend.service.RecommendationMetricService;
import com.southstand.recommend.vo.RecommendationMetricVO;
import java.time.LocalDateTime;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/internal/recommendation")
public class RecommendationMetricController {
    private final RecommendationMetricService service;
    public RecommendationMetricController(RecommendationMetricService service) { this.service = service; }

    @GetMapping("/metrics")
    public Result<RecommendationMetricVO> metrics(
            @RequestParam(required = false) String scene,
            @RequestParam(required = false) String algorithmVersion,
            @RequestParam(required = false) String experimentId,
            @RequestParam(required = false) String bucket,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        return Result.success(service.metrics(scene, algorithmVersion, experimentId, bucket, startTime, endTime));
    }
}

