package com.southstand.card.service;

import com.southstand.card.config.HomeCardProperties;
import com.southstand.card.vo.FeedCardVO;
import java.util.ArrayList;
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
        if (!properties.isEnabled() || auxiliaryCards == null || auxiliaryCards.isEmpty() || core.isEmpty()) {
            attribute(core, requestId);
            return core;
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
        attribute(result, requestId);
        return result;
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
