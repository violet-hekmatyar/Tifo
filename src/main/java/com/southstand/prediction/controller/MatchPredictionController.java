package com.southstand.prediction.controller;

import com.southstand.common.result.Result;
import com.southstand.prediction.dto.SubmitPredictionRequest;
import com.southstand.prediction.service.MatchPredictionService;
import com.southstand.prediction.vo.MatchPredictionVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/app/football/matches/{matchId}/prediction")
public class MatchPredictionController {

    private final MatchPredictionService predictionService;

    public MatchPredictionController(MatchPredictionService predictionService) {
        this.predictionService = predictionService;
    }

    @GetMapping
    public Result<MatchPredictionVO> detail(@PathVariable Long matchId) {
        return Result.success(predictionService.detail(matchId));
    }

    @PostMapping
    public Result<MatchPredictionVO> submit(
            @PathVariable Long matchId,
            @Valid @RequestBody SubmitPredictionRequest request
    ) {
        return Result.success(predictionService.submit(matchId, request));
    }
}
