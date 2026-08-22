package com.southstand.recommend.service;

import com.southstand.recommend.config.RecommendationProperties;
import com.southstand.recommend.model.RecommendationAssignment;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.springframework.stereotype.Service;

@Service
public class RecommendationExperimentService {
    public static final String BUCKET_RULE = "A";
    public static final String BUCKET_CF = "B";

    private final RecommendationProperties properties;

    public RecommendationExperimentService(RecommendationProperties properties) {
        this.properties = properties;
    }

    public RecommendationAssignment assign(Long userId) {
        String experimentId = properties.getExperiment().getId();
        if (!properties.getExperiment().isEnabled() || userId == null) {
            return new RecommendationAssignment(experimentId, BUCKET_RULE, false);
        }
        int bucket = stableBucket(experimentId, userId);
        boolean cf = bucket < properties.getExperiment().getCfPercent();
        return new RecommendationAssignment(experimentId, cf ? BUCKET_CF : BUCKET_RULE, cf);
    }

    int stableBucket(String experimentId, Long userId) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256")
                    .digest((experimentId + ":" + userId).getBytes(StandardCharsets.UTF_8));
            return Math.floorMod(ByteBuffer.wrap(hash).getInt(), 100);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}
