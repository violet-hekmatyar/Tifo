package com.southstand.recommend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.southstand.recommend.entity.UserBehaviorLog;
import com.southstand.recommend.mapper.UserBehaviorLogMapper;
import com.southstand.recommend.model.CfScoreResult;
import com.southstand.recommend.vo.RecommendationMetricVO;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.LongAdder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class RecommendationMetricService {
    private final UserBehaviorLogMapper mapper;
    private final LongAdder cfRequests = new LongAdder();
    private final LongAdder cfSuccesses = new LongAdder();
    private final LongAdder cfFallbacks = new LongAdder();
    private final LongAdder cfLatencyMs = new LongAdder();

    public RecommendationMetricService(UserBehaviorLogMapper mapper) {
        this.mapper = mapper;
    }

    public void recordCf(CfScoreResult result) {
        cfRequests.increment();
        cfLatencyMs.add(result.latencyMs());
        if (result.success()) cfSuccesses.increment(); else cfFallbacks.increment();
    }

    public RecommendationMetricVO metrics(String scene, String algorithmVersion, String experimentId,
                                           String bucket, LocalDateTime startTime, LocalDateTime endTime) {
        LocalDateTime safeEnd = endTime == null ? LocalDateTime.now() : endTime;
        LocalDateTime safeStart = startTime == null ? safeEnd.minusDays(7) : startTime;
        QueryWrapper<UserBehaviorLog> wrapper = new QueryWrapper<UserBehaviorLog>()
                .eq("status", "ACTIVE").eq("is_deleted", 0)
                .ge("event_time", safeStart).le("event_time", safeEnd)
                .last("LIMIT 100000");
        if (StringUtils.hasText(scene)) wrapper.eq("scene", scene);
        if (StringUtils.hasText(algorithmVersion)) wrapper.eq("algorithm_version", algorithmVersion);
        if (StringUtils.hasText(experimentId)) wrapper.eq("experiment_id", experimentId);
        if (StringUtils.hasText(bucket)) wrapper.eq("experiment_bucket", bucket);
        List<UserBehaviorLog> logs = mapper.selectList(wrapper);
        long expose = 0, click = 0, detail = 0, dwell = 0, like = 0, favorite = 0, comment = 0;
        if (logs != null) {
            for (UserBehaviorLog entry : logs) {
                switch (entry.getBehaviorType()) {
                    case "EXPOSE" -> expose++;
                    case "CLICK" -> click++;
                    case "DETAIL" -> { detail++; dwell += entry.getDwellMs() == null ? 0 : entry.getDwellMs(); }
                    case "LIKE" -> like++;
                    case "FAVORITE" -> favorite++;
                    case "COMMENT" -> comment++;
                    default -> { }
                }
            }
        }
        long requests = cfRequests.sum();
        RecommendationMetricVO vo = new RecommendationMetricVO();
        vo.setExposureCount(expose);
        vo.setClickCount(click);
        vo.setCtr(expose == 0 ? 0D : (double) click / expose);
        vo.setDetailCount(detail);
        vo.setAvgDwellMs(detail == 0 ? 0D : (double) dwell / detail);
        vo.setLikeCount(like);
        vo.setFavoriteCount(favorite);
        vo.setCommentCount(comment);
        vo.setInteractionRate(expose == 0 ? 0D : (double) (like + favorite + comment) / expose);
        vo.setCfRequestCount(requests);
        vo.setCfSuccessCount(cfSuccesses.sum());
        vo.setCfFallbackCount(cfFallbacks.sum());
        vo.setCfSuccessRate(requests == 0 ? 0D : (double) cfSuccesses.sum() / requests);
        vo.setFallbackRate(requests == 0 ? 0D : (double) cfFallbacks.sum() / requests);
        vo.setAvgCfLatencyMs(requests == 0 ? 0D : (double) cfLatencyMs.sum() / requests);
        return vo;
    }
}

