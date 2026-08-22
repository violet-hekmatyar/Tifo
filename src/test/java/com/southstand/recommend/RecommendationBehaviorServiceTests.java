package com.southstand.recommend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.southstand.auth.security.CurrentUserHolder;
import com.southstand.auth.security.LoginUserContext;
import com.southstand.recommend.dto.RecommendationBehaviorBatchRequest;
import com.southstand.recommend.entity.UserBehaviorLog;
import com.southstand.recommend.mapper.UserBehaviorLogMapper;
import com.southstand.recommend.service.RecommendationBehaviorService;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class RecommendationBehaviorServiceTests {
    @AfterEach void clear() { CurrentUserHolder.clear(); }

    @Test
    void duplicateClientIdsAreIdempotentAndInvalidEventsRejected() {
        UserBehaviorLogMapper mapper = mock(UserBehaviorLogMapper.class);
        when(mapper.selectList(any())).thenReturn(List.of());
        when(mapper.insert(any(UserBehaviorLog.class))).thenReturn(1);
        CurrentUserHolder.set(new LoginUserContext(7L, "demo", "USER"));
        var valid = event("same", "DETAIL", 1234L);
        var invalid = event("bad", "UNKNOWN", 0L);
        RecommendationBehaviorBatchRequest request = new RecommendationBehaviorBatchRequest();
        request.setEvents(List.of(valid, valid, invalid));
        var result = new RecommendationBehaviorService(mapper).saveBatch(request);
        assertThat(result.getSaved()).isEqualTo(1);
        assertThat(result.getDuplicated()).isEqualTo(1);
        assertThat(result.getRejected()).isEqualTo(1);
    }

    private RecommendationBehaviorBatchRequest.Event event(String id, String type, long target) {
        var event = new RecommendationBehaviorBatchRequest.Event();
        event.setClientEventId(id); event.setBehaviorType(type); event.setTargetType("CONTENT");
        event.setTargetId(target); event.setEventTime(LocalDateTime.now()); event.setDwellMs(5000L);
        return event;
    }
}
