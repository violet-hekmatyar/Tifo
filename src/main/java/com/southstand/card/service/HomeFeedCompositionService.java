package com.southstand.card.service;

import com.southstand.card.config.HomeCardProperties;
import com.southstand.card.vo.FeedCardVO;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class HomeFeedCompositionService {
    private final HomeCardProperties properties;

    public HomeFeedCompositionService(HomeCardProperties properties) {
        this.properties = properties;
    }

    public List<FeedCardVO> compose(List<FeedCardVO> coreCards, List<FeedCardVO> auxiliaryCards,
                                    String requestId) {
        List<FeedCardVO> core = deduplicate(coreCards);
        if (core.isEmpty()) {
            attribute(core, requestId);
            return core;
        }
        if (!properties.isEnabled() || auxiliaryCards == null || auxiliaryCards.isEmpty()) {
            List<FeedCardVO> arranged = arrangeRecommendPrefix(core);
            attribute(arranged, requestId);
            return arranged;
        }
        List<FeedCardVO> auxiliary = deduplicate(auxiliaryCards);
        Set<Long> discussionContentIds = new HashSet<>();
        auxiliary.stream().filter(c -> "DISCUSSION".equals(c.getCardType()) && c.getContentId() != null)
                .forEach(c -> discussionContentIds.add(c.getContentId()));
        core.removeIf(c -> "CONTENT".equals(c.getCardType()) && discussionContentIds.contains(c.getContentId()));
        int maxAux = maxAuxiliary(core.size());
        if (auxiliary.size() > maxAux) auxiliary = new ArrayList<>(auxiliary.subList(0, maxAux));
        List<FeedCardVO> result = new ArrayList<>(core.size() + auxiliary.size());
        int coreSinceAux = 0, auxiliaryIndex = 0;
        for (FeedCardVO card : core) {
            result.add(card); coreSinceAux++;
            if (auxiliaryIndex < auxiliary.size() && coreSinceAux >= properties.getMinGap()) {
                result.add(auxiliary.get(auxiliaryIndex++)); coreSinceAux = 0;
            }
        }
        result = arrangeRecommendPrefix(result);
        attribute(result, requestId);
        return result;
    }

    /**
     * Give the first recommendation page a useful mix without changing which
     * candidates exist or the relative order of cards of the same type. The
     * complete sequence is arranged before FeedService paginates it, so page
     * boundaries and exposure attribution remain stable.
     */
    private List<FeedCardVO> arrangeRecommendPrefix(List<FeedCardVO> cards) {
        if (cards.size() < 2) return cards;

        EnumMap<CardType, List<FeedCardVO>> byType = new EnumMap<>(CardType.class);
        for (CardType type : CardType.values()) byType.put(type, new ArrayList<>());
        for (FeedCardVO card : cards) {
            CardType type = CardType.from(card.getCardType());
            if (type != null) byType.get(type).add(card);
        }

        List<FeedCardVO> prefix = new ArrayList<>(10);
        Set<String> selected = new HashSet<>();
        appendNext(prefix, selected, byType.get(CardType.MATCH));
        appendNext(prefix, selected, byType.get(CardType.CONTENT));
        appendNext(prefix, selected, byType.get(CardType.RANKING));
        List<FeedCardVO> discussions = byType.get(CardType.DISCUSSION);
        if (discussions.isEmpty()) discussions = byType.get(CardType.HOT_COMMENT);
        appendNext(prefix, selected, discussions);
        appendNext(prefix, selected, byType.get(CardType.PLAYER_RATING));
        appendNext(prefix, selected, byType.get(CardType.CONTENT));
        appendNext(prefix, selected, byType.get(CardType.HOT_COMMENT));
        appendNext(prefix, selected, byType.get(CardType.CONTENT));

        for (FeedCardVO card : cards) {
            if (prefix.size() >= 10) break;
            if (selected.add(card.getCardKey())) prefix.add(card);
        }
        if (prefix.isEmpty()) return cards;

        List<FeedCardVO> arranged = new ArrayList<>(cards.size());
        arranged.addAll(prefix);
        for (FeedCardVO card : cards) {
            if (selected.add(card.getCardKey())) arranged.add(card);
        }
        return arranged;
    }

    private void appendNext(List<FeedCardVO> target, Set<String> selected, List<FeedCardVO> candidates) {
        if (candidates == null || candidates.isEmpty() || target.size() >= 10) return;
        for (FeedCardVO candidate : candidates) {
            if (selected.add(candidate.getCardKey())) {
                target.add(candidate);
                return;
            }
        }
    }

    private enum CardType {
        MATCH, CONTENT, RANKING, PLAYER_RATING, DISCUSSION, HOT_COMMENT;

        private static CardType from(String raw) {
            if (raw == null) return null;
            try {
                return valueOf(raw);
            } catch (IllegalArgumentException ignored) {
                return null;
            }
        }
    }

    private int maxAuxiliary(int coreCount) {
        double ratio = properties.getMaxRatio();
        if (ratio <= 0D || coreCount <= 0) return 0;
        return Math.max(0, (int) Math.floor(coreCount * ratio / (1D - ratio)));
    }

    private List<FeedCardVO> deduplicate(List<FeedCardVO> cards) {
        Map<String, FeedCardVO> unique = new LinkedHashMap<>();
        if (cards != null) for (FeedCardVO card : cards) {
            if (card == null) continue;
            String key = ensureKey(card);
            unique.putIfAbsent(key, card);
        }
        return new ArrayList<>(unique.values());
    }

    private void attribute(List<FeedCardVO> cards, String requestId) {
        for (int i = 0; i < cards.size(); i++) {
            FeedCardVO card = cards.get(i); String key = ensureKey(card);
            card.setPosition(i); card.setImpressionId(requestId + ":" + key);
        }
    }

    private String ensureKey(FeedCardVO card) {
        if (card.getCardKey() != null && !card.getCardKey().isBlank()) return card.getCardKey();
        String key = switch (card.getCardType()) {
            case "MATCH" -> "MATCH:" + card.getMatchId();
            case "CONTENT" -> "CONTENT:" + card.getContentId();
            case "DISCUSSION" -> "DISCUSSION:" + card.getContentId();
            default -> card.getCardType() + ":" + card.getCardId();
        };
        card.setCardKey(key); return key;
    }
}
