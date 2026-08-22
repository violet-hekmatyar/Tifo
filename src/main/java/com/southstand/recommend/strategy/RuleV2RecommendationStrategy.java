package com.southstand.recommend.strategy;

import com.southstand.recommend.model.RecommendationCandidate;
import com.southstand.recommend.model.RecommendationContext;
import com.southstand.recommend.model.RecommendationItem;
import com.southstand.recommend.model.RecommendationReasonCode;
import com.southstand.recommend.model.RecommendationTargetType;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class RuleV2RecommendationStrategy implements RecommendationStrategy {
    public static final String VERSION = "RULE_V2";

    private static final double MAIN_TEAM = 50D;
    private static final double FOLLOW_TEAM = 35D;
    private static final double FOLLOW_PLAYER = 25D;
    private static final double FOLLOW_AUTHOR = 45D;
    private static final double ENGAGEMENT_CAP = 30D;
    private static final double ENGAGEMENT_SCALE = 200D;
    private static final double EXPOSED_PENALTY = 100D;

    @Override
    public List<RecommendationItem> rank(RecommendationContext context) {
        LocalDateTime now = LocalDateTime.now();
        List<RecommendationItem> result = new ArrayList<>();
        for (RecommendationCandidate candidate : context.getCandidates()) {
            if (candidate == null || candidate.getTargetType() == null || candidate.getTargetId() == null) {
                continue;
            }
            RecommendationItem item = candidate.getTargetType() == RecommendationTargetType.MATCH
                    ? scoreMatch(candidate, context, now) : scoreContent(candidate, context, now);
            if (context.getExposedKeys().contains(candidate.key())) {
                item.setScore(item.getScore() - EXPOSED_PENALTY);
                item.setRuleScore(item.getRuleScore() - EXPOSED_PENALTY);
            }
            result.add(item);
        }
        result.sort(itemComparator(context));
        return result;
    }

    private RecommendationItem scoreContent(RecommendationCandidate c, RecommendationContext ctx,
                                             LocalDateTime now) {
        double rawEngagement = c.getLikeCount() * 2D + c.getCommentCount() * 3D + c.getFavoriteCount() * 4D;
        double score = c.getHotScore() + engagementScore(rawEngagement) + timeBoost(c.getPublishTime(), now);
        RecommendationReasonCode reason = RecommendationReasonCode.TRENDING;
        if (rawEngagement <= 0 && ageHours(c.getPublishTime(), now) >= 0
                && ageHours(c.getPublishTime(), now) <= 6) {
            score += 10D;
            reason = RecommendationReasonCode.FRESH_CONTENT;
        }
        if (ctx.getMainTeamId() != null && c.getTeamIds().contains(ctx.getMainTeamId())) {
            score += MAIN_TEAM;
            reason = RecommendationReasonCode.MAIN_TEAM;
        }
        if (c.getAuthorId() != null && ctx.getFollowedUserIds().contains(c.getAuthorId())) {
            score += FOLLOW_AUTHOR;
            if (reason != RecommendationReasonCode.MAIN_TEAM) {
                reason = RecommendationReasonCode.FOLLOWED_AUTHOR;
            }
        }
        if (intersects(c.getTeamIds(), ctx.getFollowedTeamIds())) {
            score += FOLLOW_TEAM;
            if (reason != RecommendationReasonCode.MAIN_TEAM
                    && reason != RecommendationReasonCode.FOLLOWED_AUTHOR) {
                reason = RecommendationReasonCode.FOLLOWED_TEAM;
            }
        }
        if (intersects(c.getPlayerIds(), ctx.getFollowedPlayerIds())) {
            score += FOLLOW_PLAYER;
            if (reason != RecommendationReasonCode.MAIN_TEAM
                    && reason != RecommendationReasonCode.FOLLOWED_AUTHOR
                    && reason != RecommendationReasonCode.FOLLOWED_TEAM) {
                reason = RecommendationReasonCode.FOLLOWED_PLAYER;
            }
        }
        return item(c, score, reason);
    }

    private RecommendationItem scoreMatch(RecommendationCandidate c, RecommendationContext ctx,
                                           LocalDateTime now) {
        double score = 0D;
        RecommendationReasonCode reason = RecommendationReasonCode.IMPORTANT_MATCH;
        if ("LIVE".equals(c.getMatchStatus())) {
            score += 80D;
            reason = RecommendationReasonCode.LIVE_MATCH;
        } else if ("SCHEDULED".equals(c.getMatchStatus())) {
            score += 40D + proximityBoost(c.getMatchTime(), now);
            reason = RecommendationReasonCode.UPCOMING_MATCH;
        } else if ("FINISHED".equals(c.getMatchStatus()) && c.isHasReport()) {
            score += postMatchBoost(c.getMatchTime(), now);
        }
        if (c.getImportantLevel() != null && c.getImportantLevel() > 0) {
            score += 50D + c.getImportantLevel() * 5D;
            if (reason != RecommendationReasonCode.LIVE_MATCH) {
                reason = RecommendationReasonCode.IMPORTANT_MATCH;
            }
        }
        if (ctx.getMainTeamId() != null && (Objects.equals(ctx.getMainTeamId(), c.getHomeTeamId())
                || Objects.equals(ctx.getMainTeamId(), c.getAwayTeamId()))) {
            score += MAIN_TEAM;
            reason = RecommendationReasonCode.MAIN_TEAM;
        } else if (ctx.getFollowedTeamIds().contains(c.getHomeTeamId())
                || ctx.getFollowedTeamIds().contains(c.getAwayTeamId())) {
            score += FOLLOW_TEAM;
            if (reason != RecommendationReasonCode.LIVE_MATCH) {
                reason = RecommendationReasonCode.FOLLOWED_TEAM;
            }
        }
        return item(c, score, reason);
    }

    private RecommendationItem item(RecommendationCandidate c, double score, RecommendationReasonCode reason) {
        RecommendationItem item = new RecommendationItem();
        item.setTargetType(c.getTargetType());
        item.setTargetId(c.getTargetId());
        item.setScore(score);
        item.setRuleScore(score);
        item.setReasonCode(reason);
        return item;
    }

    private Comparator<RecommendationItem> itemComparator(RecommendationContext context) {
        return Comparator.comparingDouble(RecommendationItem::getScore).reversed()
                .thenComparing((RecommendationItem item) -> publishTime(item, context),
                        Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(RecommendationItem::getTargetId);
    }

    private LocalDateTime publishTime(RecommendationItem item, RecommendationContext context) {
        for (RecommendationCandidate candidate : context.getCandidates()) {
            if (candidate.getTargetType() == item.getTargetType()
                    && Objects.equals(candidate.getTargetId(), item.getTargetId())) {
                return candidate.getTargetType() == RecommendationTargetType.CONTENT
                        ? candidate.getPublishTime() : candidate.getMatchTime();
            }
        }
        return null;
    }

    private double engagementScore(double raw) {
        return raw <= 0 ? 0D : Math.min(ENGAGEMENT_CAP,
                ENGAGEMENT_CAP * Math.log1p(raw) / Math.log1p(ENGAGEMENT_SCALE));
    }

    private double timeBoost(LocalDateTime time, LocalDateTime now) {
        double hours = ageHours(time, now);
        if (hours < 0) return time == null ? 0D : 30D;
        return 30D * Math.exp(-hours / 24D);
    }

    private double ageHours(LocalDateTime time, LocalDateTime now) {
        if (time == null) return -1D;
        return Duration.between(time, now).toMinutes() / 60D;
    }

    private double proximityBoost(LocalDateTime time, LocalDateTime now) {
        if (time == null) return 0D;
        double hours = Duration.between(now, time).toMinutes() / 60D;
        return hours < 0 ? 20D : 20D * Math.exp(-hours / 48D);
    }

    private double postMatchBoost(LocalDateTime time, LocalDateTime now) {
        if (time == null) return 25D;
        double hours = Duration.between(time.plusHours(2), now).toMinutes() / 60D;
        return hours < 0 ? 25D : 25D * Math.exp(-hours / 48D);
    }

    private boolean intersects(Set<Long> left, Set<Long> right) {
        if (left == null || right == null || left.isEmpty() || right.isEmpty()) return false;
        for (Long value : left) if (right.contains(value)) return true;
        return false;
    }
}

