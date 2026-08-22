package com.southstand.recommend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.southstand.recommend.client.RecommendationRemoteClient;
import com.southstand.recommend.config.RecommendationProperties;
import com.southstand.recommend.model.RecommendationAssignment;
import com.southstand.recommend.model.RecommendationCandidate;
import com.southstand.recommend.model.RecommendationContext;
import com.southstand.recommend.model.RecommendationTargetType;
import com.southstand.recommend.service.RecommendationExperimentService;
import com.southstand.recommend.service.RecommendationMetricService;
import com.southstand.recommend.service.RecommendationMixService;
import com.southstand.recommend.service.RecommendationService;
import com.southstand.recommend.strategy.RuleV2RecommendationStrategy;
import java.util.List;
import org.junit.jupiter.api.Test;

class RecommendationServiceTests {
    @Test
    void ruleFailureFallsBackToHotWithoutCallingCfEvenForBucketB() {
        RecommendationExperimentService experiment = mock(RecommendationExperimentService.class);
        when(experiment.assign(1L)).thenReturn(new RecommendationAssignment("REC_HOME_V1", "B", true));
        RuleV2RecommendationStrategy rule = mock(RuleV2RecommendationStrategy.class);
        when(rule.rank(any())).thenThrow(new IllegalStateException("rule failed"));
        RecommendationRemoteClient remote = mock(RecommendationRemoteClient.class);
        RecommendationCandidate candidate = new RecommendationCandidate();
        candidate.setTargetType(RecommendationTargetType.CONTENT); candidate.setTargetId(2L); candidate.setLikeCount(5);
        RecommendationContext context = new RecommendationContext(); context.setUserId(1L);
        context.setCandidates(List.of(candidate)); context.setScene("HOME_RECOMMEND");
        var service = new RecommendationService(experiment, rule, remote, new RecommendationMixService(),
                mock(RecommendationMetricService.class), new RecommendationProperties());
        var result = service.recommend(context);
        assertThat(result.getAlgorithmVersion()).isEqualTo("HOT_V1");
        assertThat(result.getExperimentBucket()).isEqualTo("B");
        assertThat(result.getItems()).hasSize(1);
        verifyNoInteractions(remote);
    }
}
