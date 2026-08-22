package com.southstand.recommend.service;

import com.southstand.recommend.client.RecommendationRemoteClient;
import com.southstand.recommend.config.RecommendationProperties;
import com.southstand.recommend.model.CfScoreResult;
import com.southstand.recommend.model.RecommendationAssignment;
import com.southstand.recommend.model.RecommendationCandidate;
import com.southstand.recommend.model.RecommendationContext;
import com.southstand.recommend.model.RecommendationItem;
import com.southstand.recommend.model.RecommendationReasonCode;
import com.southstand.recommend.model.RecommendationTargetType;
import com.southstand.recommend.strategy.RuleV2RecommendationStrategy;
import com.southstand.recommend.vo.RecommendationResult;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class RecommendationService {
    private static final Logger log = LoggerFactory.getLogger(RecommendationService.class);
    public static final String CF_VERSION = "CF_V1";
    public static final String HOT_VERSION = "HOT_V1";

    private final RecommendationExperimentService experimentService;
    private final RuleV2RecommendationStrategy ruleStrategy;
    private final RecommendationRemoteClient remoteClient;
    private final RecommendationMixService mixService;
    private final RecommendationMetricService metricService;
    private final RecommendationProperties properties;

    public RecommendationService(RecommendationExperimentService experimentService,
                                 RuleV2RecommendationStrategy ruleStrategy,
                                 RecommendationRemoteClient remoteClient,
                                 RecommendationMixService mixService,
                                 RecommendationMetricService metricService,
                                 RecommendationProperties properties) {
        this.experimentService = experimentService;
        this.ruleStrategy = ruleStrategy;
        this.remoteClient = remoteClient;
        this.mixService = mixService;
        this.metricService = metricService;
        this.properties = properties;
    }

    public RecommendationResult recommend(RecommendationContext context) {
        RecommendationAssignment assignment = experimentService.assign(context.getUserId());
        RecommendationResult result = new RecommendationResult();
        result.setExperimentId(assignment.experimentId());
        result.setExperimentBucket(assignment.bucket());
        result.setRequestId(UUID.randomUUID().toString());

        List<RecommendationItem> ranked;
        boolean ruleAvailable = true;
        try {
            ranked = ruleStrategy.rank(context);
            result.setAlgorithmVersion(RuleV2RecommendationStrategy.VERSION);
        } catch (RuntimeException e) {
            ruleAvailable = false;
            log.warn("RULE_V2 failed, fallback to HOT: {}", e.getMessage());
            ranked = hotRank(context);
            result.setAlgorithmVersion(HOT_VERSION);
            result.setFallbackReason("RULE_V2_ERROR");
        }

        if (ruleAvailable && assignment.useCf() && !ranked.isEmpty()) {
            List<Long> contentIds = context.getCandidates().stream()
                    .filter(c -> c.getTargetType() == RecommendationTargetType.CONTENT)
                    .map(RecommendationCandidate::getTargetId).toList();
            if (!contentIds.isEmpty()) {
                CfScoreResult cf = remoteClient.contentScores(context.getUserId(), contentIds);
                metricService.recordCf(cf);
                if (cf.success()) {
                    mergeCf(ranked, cf.scores());
                    result.setAlgorithmVersion(CF_VERSION);
                    result.setModelVersion(cf.modelVersion());
                } else {
                    result.setCfFallback(true);
                    result.setFallbackReason(cf.failureReason());
                }
            }
        }
        sortStable(ranked, context);
        result.setItems(mixService.mix(ranked, context.getCandidates(), context.getScene()));
        return result;
    }

    private void mergeCf(List<RecommendationItem> ranked, Map<Long, Double> scores) {
        for (RecommendationItem item : ranked) {
            if (item.getTargetType() != RecommendationTargetType.CONTENT) continue;
            double cfScore = scores.getOrDefault(item.getTargetId(), 0D);
            item.setCfScore(cfScore);
            item.setScore(item.getRuleScore() + properties.getCf().getWeight() * cfScore);
            if (cfScore >= 0.30D && (item.getReasonCode() == RecommendationReasonCode.TRENDING
                    || item.getReasonCode() == RecommendationReasonCode.FRESH_CONTENT)) {
                item.setReasonCode(RecommendationReasonCode.SIMILAR_CONTENT);
            }
        }
    }

    private List<RecommendationItem> hotRank(RecommendationContext context) {
        List<RecommendationItem> items = new ArrayList<>();
        for (RecommendationCandidate c : context.getCandidates()) {
            RecommendationItem item = new RecommendationItem();
            item.setTargetType(c.getTargetType());
            item.setTargetId(c.getTargetId());
            double score = c.getTargetType() == RecommendationTargetType.CONTENT
                    ? c.getHotScore() + c.getLikeCount() * 2D + c.getCommentCount() * 3D
                    + c.getFavoriteCount() * 4D
                    : (c.getImportantLevel() == null ? 0D : c.getImportantLevel() * 5D);
            item.setScore(score);
            item.setRuleScore(score);
            item.setReasonCode(c.getTargetType() == RecommendationTargetType.CONTENT
                    ? RecommendationReasonCode.TRENDING : RecommendationReasonCode.IMPORTANT_MATCH);
            items.add(item);
        }
        sortStable(items, context);
        return items;
    }

    private void sortStable(List<RecommendationItem> items, RecommendationContext context) {
        Map<String, RecommendationCandidate> candidates = new HashMap<>();
        for (RecommendationCandidate candidate : context.getCandidates()) candidates.put(candidate.key(), candidate);
        items.sort(Comparator.comparingDouble(RecommendationItem::getScore).reversed()
                .thenComparing(item -> timeOf(candidates.get(item.key())),
                        Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(RecommendationItem::getTargetId));
    }

    private LocalDateTime timeOf(RecommendationCandidate candidate) {
        if (candidate == null) return null;
        return candidate.getTargetType() == RecommendationTargetType.CONTENT
                ? candidate.getPublishTime() : candidate.getMatchTime();
    }
}
