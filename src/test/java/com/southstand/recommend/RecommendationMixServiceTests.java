package com.southstand.recommend;

import static org.assertj.core.api.Assertions.assertThat;

import com.southstand.recommend.model.RecommendationCandidate;
import com.southstand.recommend.model.RecommendationItem;
import com.southstand.recommend.model.RecommendationTargetType;
import com.southstand.recommend.service.RecommendationMixService;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class RecommendationMixServiceTests {
    @Test
    void homeUsesSevenToThreeBlocksAndAvoidsThreeSameAuthors() {
        List<RecommendationItem> items = new ArrayList<>();
        List<RecommendationCandidate> candidates = new ArrayList<>();
        for (long id = 1; id <= 10; id++) add(items, candidates, RecommendationTargetType.CONTENT, id, id <= 3 ? 1L : id);
        for (long id = 101; id <= 104; id++) add(items, candidates, RecommendationTargetType.MATCH, id, null);
        var mixed = new RecommendationMixService().mix(items, candidates, "HOME_RECOMMEND");
        assertThat(mixed.subList(0, 7)).allMatch(i -> i.getTargetType() == RecommendationTargetType.CONTENT);
        assertThat(mixed.subList(7, 10)).allMatch(i -> i.getTargetType() == RecommendationTargetType.MATCH);
        assertThat(mixed.subList(0, 3).stream().map(RecommendationItem::getTargetId).toList())
                .isNotEqualTo(List.of(1L, 2L, 3L));
    }

    private void add(List<RecommendationItem> items, List<RecommendationCandidate> candidates,
                     RecommendationTargetType type, long id, Long author) {
        RecommendationItem item = new RecommendationItem(); item.setTargetType(type); item.setTargetId(id); items.add(item);
        RecommendationCandidate candidate = new RecommendationCandidate(); candidate.setTargetType(type);
        candidate.setTargetId(id); candidate.setAuthorId(author); candidates.add(candidate);
    }
}
