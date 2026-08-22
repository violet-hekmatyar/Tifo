package com.southstand.recommend.model;

import java.util.Map;

public record CfScoreResult(boolean success, String modelVersion, Map<Long, Double> scores,
                            String failureReason, long latencyMs) {
    public static CfScoreResult failure(String reason, long latencyMs) {
        return new CfScoreResult(false, null, Map.of(), reason, latencyMs);
    }
}

