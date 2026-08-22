package com.southstand.recommend.service;

import com.southstand.recommend.model.RecommendationCandidate;
import com.southstand.recommend.model.RecommendationItem;
import com.southstand.recommend.model.RecommendationTargetType;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class RecommendationMixService {
    private static final int CONTENT_QUOTA = 7;
    private static final int MATCH_QUOTA = 3;
    private static final int LOOKAHEAD = 6;

    public List<RecommendationItem> mix(List<RecommendationItem> ranked,
                                        List<RecommendationCandidate> candidates, String scene) {
        if (ranked == null || ranked.isEmpty()) return List.of();
        Map<String, RecommendationCandidate> byKey = new HashMap<>();
        for (RecommendationCandidate candidate : candidates) byKey.put(candidate.key(), candidate);
        if (!"HOME_RECOMMEND".equals(scene) && !"FOLLOWING_FEED".equals(scene)) {
            return diversify(new ArrayDeque<>(ranked), byKey);
        }
        Deque<RecommendationItem> content = new ArrayDeque<>();
        Deque<RecommendationItem> match = new ArrayDeque<>();
        for (RecommendationItem item : ranked) {
            (item.getTargetType() == RecommendationTargetType.MATCH ? match : content).addLast(item);
        }
        if (content.isEmpty() || match.isEmpty()) return diversify(new ArrayDeque<>(ranked), byKey);
        List<RecommendationItem> output = new ArrayList<>(ranked.size());
        while (!content.isEmpty() || !match.isEmpty()) {
            for (int i = 0; i < CONTENT_QUOTA && !content.isEmpty(); i++) {
                output.add(pollDiverse(content, output, byKey));
            }
            for (int i = 0; i < MATCH_QUOTA && !match.isEmpty(); i++) {
                output.add(pollDiverse(match, output, byKey));
            }
            if (content.isEmpty() && !match.isEmpty() && output.size() < ranked.size()) {
                output.add(pollDiverse(match, output, byKey));
            }
        }
        return output;
    }

    private List<RecommendationItem> diversify(Deque<RecommendationItem> pool,
                                                Map<String, RecommendationCandidate> byKey) {
        List<RecommendationItem> output = new ArrayList<>(pool.size());
        while (!pool.isEmpty()) output.add(pollDiverse(pool, output, byKey));
        return output;
    }

    private RecommendationItem pollDiverse(Deque<RecommendationItem> pool, List<RecommendationItem> output,
                                             Map<String, RecommendationCandidate> byKey) {
        if (output.isEmpty()) return pool.removeFirst();
        List<RecommendationItem> held = new ArrayList<>();
        RecommendationItem picked = null;
        int count = Math.min(LOOKAHEAD, pool.size());
        for (int i = 0; i < count; i++) {
            RecommendationItem current = pool.removeFirst();
            if (!conflicts(current, output, byKey)) {
                picked = current;
                break;
            }
            held.add(current);
        }
        if (picked == null) picked = held.remove(0);
        for (int i = held.size() - 1; i >= 0; i--) pool.addFirst(held.get(i));
        return picked;
    }

    private boolean conflicts(RecommendationItem current, List<RecommendationItem> output,
                              Map<String, RecommendationCandidate> byKey) {
        RecommendationCandidate candidate = byKey.get(current.key());
        if (candidate == null) return false;
        int sameAuthor = 0;
        int windowStart = Math.max(0, output.size() - 4);
        for (int i = windowStart; i < output.size(); i++) {
            RecommendationCandidate previous = byKey.get(output.get(i).key());
            if (previous == null) continue;
            if (candidate.getAuthorId() != null && Objects.equals(candidate.getAuthorId(), previous.getAuthorId())) {
                sameAuthor++;
            }
            Long currentMatch = matchId(candidate);
            if (currentMatch != null && Objects.equals(currentMatch, matchId(previous))) return true;
        }
        return sameAuthor >= 2;
    }

    private Long matchId(RecommendationCandidate candidate) {
        return candidate.getTargetType() == RecommendationTargetType.MATCH
                ? candidate.getTargetId() : candidate.getRelatedMatchId();
    }
}
