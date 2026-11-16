package com.southstand.prediction.vo;

import java.time.LocalDateTime;
import java.util.List;

public record MatchPredictionVO(
        long matchId,
        String matchStatus,
        String window,
        boolean canSubmit,
        String lockReason,
        String leagueName,
        LocalDateTime matchTime,
        Integer homeScore,
        Integer awayScore,
        TeamSide homeTeam,
        TeamSide awayTeam,
        Consensus consensus,
        MyPrediction myPrediction,
        Challenge challenge,
        PostMatch postMatch,
        List<TrajectoryPoint> trajectory,
        List<Market> morePredictions,
        String disclaimer
) {

    public record TeamSide(long id, String name, String shortName, String logoUrl) {
    }

    public record Consensus(int homePercent, int drawPercent, int awayPercent, int participantCount) {
    }

    public record MyPrediction(
            long id,
            int homeProbability,
            int drawProbability,
            int awayProbability,
            String predictedOutcome,
            String predictedOutcomeLabel,
            Integer insightScore,
            int stake,
            String challengeResult,
            Integer rewardBeans,
            LocalDateTime submittedAt,
            LocalDateTime settledAt
    ) {
    }

    public record Challenge(
            boolean enabled,
            Integer balance,
            List<Integer> stakes,
            int minStake,
            int maxStake,
            String ruleText
    ) {
    }

    public record PostMatch(
            Integer score,
            String calibrationLabel,
            boolean hit,
            Integer rewardBeans,
            Integer balance,
            String headline,
            String actualOutcome,
            String actualOutcomeLabel,
            List<String> highlights
    ) {
    }

    public record TrajectoryPoint(
            String stageLabel,
            int homePercent,
            int drawPercent,
            int awayPercent,
            String outcomeLabel
    ) {
    }

    public record Market(String label, int participantCount, int percent) {
    }
}
