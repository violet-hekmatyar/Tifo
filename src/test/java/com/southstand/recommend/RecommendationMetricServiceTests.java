package com.southstand.recommend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.southstand.recommend.entity.UserBehaviorLog;
import com.southstand.recommend.mapper.UserBehaviorLogMapper;
import com.southstand.recommend.model.CfScoreResult;
import com.southstand.recommend.service.RecommendationMetricService;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class RecommendationMetricServiceTests {
    @Test
    void computesBehaviorAndCfRates() {
        UserBehaviorLogMapper mapper = mock(UserBehaviorLogMapper.class);
        when(mapper.selectList(any())).thenReturn(List.of(log("EXPOSE", null), log("EXPOSE", null),
                log("CLICK", null), log("DETAIL", 6000L), log("LIKE", null)));
        var service = new RecommendationMetricService(mapper);
        service.recordCf(new CfScoreResult(true, "v", Map.of(1L, .5), null, 10));
        service.recordCf(CfScoreResult.failure("down", 20));
        var value = service.metrics(null, null, null, null, null, null);
        assertThat(value.getCtr()).isEqualTo(.5);
        assertThat(value.getAvgDwellMs()).isEqualTo(6000);
        assertThat(value.getCfSuccessRate()).isEqualTo(.5);
        assertThat(value.getFallbackRate()).isEqualTo(.5);
    }

    private UserBehaviorLog log(String type, Long dwell) {
        UserBehaviorLog value = new UserBehaviorLog(); value.setBehaviorType(type); value.setDwellMs(dwell); return value;
    }
}
