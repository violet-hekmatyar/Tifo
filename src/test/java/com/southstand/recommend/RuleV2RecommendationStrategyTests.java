package com.southstand.recommend;

import static org.assertj.core.api.Assertions.assertThat;

import com.southstand.recommend.model.RecommendationCandidate;
import com.southstand.recommend.model.RecommendationContext;
import com.southstand.recommend.model.RecommendationReasonCode;
import com.southstand.recommend.model.RecommendationTargetType;
import com.southstand.recommend.strategy.RuleV2RecommendationStrategy;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class RuleV2RecommendationStrategyTests {
    @Test
    void mainTeamOutranksHotGenericContentAndExposedItemIsPenalized() {
        RecommendationCandidate main = content(1L, 10L, 1L);
        RecommendationCandidate generic = content(2L, 20L, 2L);
        generic.setLikeCount(10000);
        RecommendationContext context = new RecommendationContext();
        context.setMainTeamId(10L);
        context.setExposedKeys(Set.of(main.key()));
        context.setCandidates(List.of(main, generic));

        var ranked = new RuleV2RecommendationStrategy().rank(context);

        assertThat(ranked.get(0).getTargetId()).isEqualTo(2L);
        assertThat(ranked.get(1).getReasonCode()).isEqualTo(RecommendationReasonCode.MAIN_TEAM);
        assertThat(ranked.get(1).getScore()).isLessThan(ranked.get(0).getScore());
    }

    @Test
    void liveMatchGetsExplainableReason() {
        RecommendationCandidate match = new RecommendationCandidate();
        match.setTargetType(RecommendationTargetType.MATCH);
        match.setTargetId(9L);
        match.setMatchStatus("LIVE");
        RecommendationContext context = new RecommendationContext();
        context.setCandidates(List.of(match));
        assertThat(new RuleV2RecommendationStrategy().rank(context).get(0).getReasonCode())
                .isEqualTo(RecommendationReasonCode.LIVE_MATCH);
    }

    private RecommendationCandidate content(long id, long team, long author) {
        RecommendationCandidate c = new RecommendationCandidate();
        c.setTargetType(RecommendationTargetType.CONTENT);
        c.setTargetId(id);
        c.setTeamIds(Set.of(team));
        c.setAuthorId(author);
        c.setPublishTime(LocalDateTime.now().minusHours(12));
        return c;
    }
}
