package com.southstand.prediction.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record SubmitPredictionRequest(
        @NotNull @Min(0) @Max(100) Integer homeProbability,
        @NotNull @Min(0) @Max(100) Integer drawProbability,
        @NotNull @Min(0) @Max(100) Integer awayProbability,
        Integer stake
) {
}
