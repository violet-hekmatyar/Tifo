package com.southstand.recommend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.southstand.recommend.controller.RecommendationBehaviorController;
import com.southstand.recommend.dto.RecommendationBehaviorBatchRequest;
import com.southstand.recommend.service.RecommendationBehaviorService;
import com.southstand.recommend.vo.RecommendationBehaviorBatchResult;
import org.junit.jupiter.api.Test;

class RecommendationBehaviorControllerTests {
    @Test
    void returnsActualBatchCounters() {
        RecommendationBehaviorService service = mock(RecommendationBehaviorService.class);
        RecommendationBehaviorBatchResult counters = new RecommendationBehaviorBatchResult();
        counters.setReceived(3); counters.setSaved(2); counters.setDuplicated(1);
        when(service.saveBatch(any())).thenReturn(counters);
        var result = new RecommendationBehaviorController(service).batch(new RecommendationBehaviorBatchRequest());
        assertThat(result.getData()).isSameAs(counters);
    }
}
