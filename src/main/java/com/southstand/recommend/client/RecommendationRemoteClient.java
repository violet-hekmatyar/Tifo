package com.southstand.recommend.client;

import com.southstand.recommend.config.RecommendationProperties;
import com.southstand.recommend.dto.CfContentScoreRequest;
import com.southstand.recommend.dto.CfContentScoreResponse;
import com.southstand.recommend.model.CfScoreResult;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

@Component
public class RecommendationRemoteClient {
    private static final Logger log = LoggerFactory.getLogger(RecommendationRemoteClient.class);
    private static final String PATH = "/api/internal/recommend/content-scores";

    private final RecommendationProperties properties;
    private final RestClient client;
    private final AtomicInteger consecutiveFailures = new AtomicInteger();
    private final AtomicLong cooldownUntilEpochMs = new AtomicLong();

    public RecommendationRemoteClient(RecommendationProperties properties) {
        this.properties = properties;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofMillis(properties.getCf().getConnectTimeoutMs()));
        factory.setReadTimeout(Duration.ofMillis(properties.getCf().getReadTimeoutMs()));
        this.client = RestClient.builder()
                .baseUrl(properties.getCf().getBaseUrl())
                .requestFactory(factory)
                .build();
    }

    public CfScoreResult contentScores(Long userId, List<Long> candidateIds) {
        long start = System.nanoTime();
        if (!properties.isEnabled() || !properties.getCf().isEnabled()) {
            return CfScoreResult.failure("CF_DISABLED", elapsed(start));
        }
        if (userId == null || candidateIds == null || candidateIds.isEmpty()) {
            return CfScoreResult.failure("NO_CF_INPUT", elapsed(start));
        }
        if (System.currentTimeMillis() < cooldownUntilEpochMs.get()) {
            return CfScoreResult.failure("CF_COOLDOWN", elapsed(start));
        }
        Set<Long> allowed = candidateIds.stream().collect(Collectors.toSet());
        try {
            CfContentScoreResponse response = client.post().uri(PATH)
                    .body(new CfContentScoreRequest(userId, candidateIds))
                    .retrieve().body(CfContentScoreResponse.class);
            String invalid = validate(response, allowed);
            if (invalid != null) return fail(invalid, start);
            Map<Long, Double> scores = new LinkedHashMap<>();
            for (CfContentScoreResponse.Item item : response.getItems()) {
                scores.put(item.getContentId(), item.getCfScore());
            }
            consecutiveFailures.set(0);
            cooldownUntilEpochMs.set(0L);
            return new CfScoreResult(true, response.getModelVersion(), scores, null, elapsed(start));
        } catch (RuntimeException e) {
            log.warn("CF request failed, fallback to rule: {}", e.getMessage());
            return fail("CF_REQUEST_ERROR", start);
        }
    }

    private String validate(CfContentScoreResponse response, Set<Long> allowed) {
        if (response == null) return "CF_EMPTY_RESPONSE";
        if (!response.isSuccess()) return "CF_UNSUCCESSFUL";
        if (!response.isModelReady()) return "CF_MODEL_NOT_READY";
        if (!StringUtils.hasText(response.getModelVersion())) return "CF_MODEL_VERSION_MISSING";
        if (response.getItems() == null || response.getItems().isEmpty()) return "CF_EMPTY_ITEMS";
        for (CfContentScoreResponse.Item item : response.getItems()) {
            if (item == null || item.getContentId() == null || !allowed.contains(item.getContentId())) {
                return "CF_ILLEGAL_CONTENT_ID";
            }
            Double score = item.getCfScore();
            if (score == null || !Double.isFinite(score) || score < 0D || score > 1D) {
                return "CF_ILLEGAL_SCORE";
            }
        }
        return null;
    }

    private CfScoreResult fail(String reason, long start) {
        int failures = consecutiveFailures.incrementAndGet();
        if (failures >= properties.getCf().getFailureThreshold()) {
            cooldownUntilEpochMs.set(System.currentTimeMillis()
                    + properties.getCf().getCooldownSeconds() * 1000L);
            consecutiveFailures.set(0);
        }
        return CfScoreResult.failure(reason, elapsed(start));
    }

    private long elapsed(long start) {
        return Math.max(0L, (System.nanoTime() - start) / 1_000_000L);
    }

    int consecutiveFailures() { return consecutiveFailures.get(); }
    long cooldownUntilEpochMs() { return cooldownUntilEpochMs.get(); }
}
