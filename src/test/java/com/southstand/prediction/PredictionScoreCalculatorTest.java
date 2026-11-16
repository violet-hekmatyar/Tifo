package com.southstand.prediction;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.southstand.prediction.service.PredictionScoreCalculator;
import org.junit.jupiter.api.Test;

class PredictionScoreCalculatorTest {

    @Test
    void homeWinExampleMatchesProductRule() {
        assertEquals(90, PredictionScoreCalculator.score(64, 22, 14, "HOME"));
        assertEquals(40, PredictionScoreCalculator.score(64, 22, 14, "AWAY"));
        assertEquals(48, PredictionScoreCalculator.score(64, 22, 14, "DRAW"));
    }

    @Test
    void challengeRewardUsesInsightScore() {
        assertEquals(30, PredictionScoreCalculator.reward(50, 90));
        assertEquals(18, PredictionScoreCalculator.reward(30, 90));
        assertEquals(0, PredictionScoreCalculator.reward(50, 60));
        assertEquals(40, PredictionScoreCalculator.reward(50, 100));
    }

    @Test
    void outcomeUsesHighestProbabilityWithStableTieBreak() {
        assertEquals("HOME", PredictionScoreCalculator.outcomeOf(45, 30, 25));
        assertEquals("DRAW", PredictionScoreCalculator.outcomeOf(30, 45, 25));
        assertEquals("AWAY", PredictionScoreCalculator.outcomeOf(30, 25, 45));
        assertEquals("HOME", PredictionScoreCalculator.outcomeOf(40, 40, 20));
    }

    @Test
    void actualOutcomeFromScore() {
        assertEquals("HOME", PredictionScoreCalculator.actualOutcome(4, 1));
        assertEquals("DRAW", PredictionScoreCalculator.actualOutcome(2, 2));
        assertEquals("AWAY", PredictionScoreCalculator.actualOutcome(0, 1));
    }
}
