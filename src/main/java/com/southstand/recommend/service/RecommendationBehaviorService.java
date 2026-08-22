package com.southstand.recommend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.southstand.auth.security.CurrentUserHolder;
import com.southstand.recommend.dto.RecommendationBehaviorBatchRequest;
import com.southstand.recommend.entity.UserBehaviorLog;
import com.southstand.recommend.mapper.UserBehaviorLogMapper;
import com.southstand.recommend.model.RecommendationBehaviorType;
import com.southstand.recommend.model.RecommendationTargetType;
import com.southstand.recommend.vo.RecommendationBehaviorBatchResult;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

@Service
public class RecommendationBehaviorService {

    private static final Logger log = LoggerFactory.getLogger(RecommendationBehaviorService.class);
    private static final String ACTIVE = "ACTIVE";
    private static final int NOT_DELETED = 0;

    private final UserBehaviorLogMapper mapper;

    public RecommendationBehaviorService(UserBehaviorLogMapper mapper) {
        this.mapper = mapper;
    }

    public RecommendationBehaviorBatchResult saveBatch(RecommendationBehaviorBatchRequest request) {
        Long userId = CurrentUserHolder.get().getUserId();
        List<RecommendationBehaviorBatchRequest.Event> events = request.getEvents();
        RecommendationBehaviorBatchResult result = new RecommendationBehaviorBatchResult();
        result.setReceived(events.size());

        Set<String> clientIds = new HashSet<>();
        for (RecommendationBehaviorBatchRequest.Event event : events) {
            if (event != null && event.getClientEventId() != null) {
                clientIds.add(event.getClientEventId());
            }
        }
        Set<String> existing = existingClientEventIds(clientIds);
        Set<String> seenInRequest = new HashSet<>();
        for (RecommendationBehaviorBatchRequest.Event event : events) {
            if (!valid(event)) {
                result.setRejected(result.getRejected() + 1);
                continue;
            }
            if (existing.contains(event.getClientEventId()) || !seenInRequest.add(event.getClientEventId())) {
                result.setDuplicated(result.getDuplicated() + 1);
                continue;
            }
            try {
                mapper.insert(toEntity(userId, event));
                result.setSaved(result.getSaved() + 1);
            } catch (DuplicateKeyException e) {
                result.setDuplicated(result.getDuplicated() + 1);
            } catch (RuntimeException e) {
                log.warn("recommendation behavior insert failed, clientEventId={}: {}",
                        event.getClientEventId(), e.getMessage());
                result.setRejected(result.getRejected() + 1);
            }
        }
        return result;
    }

    public Set<String> recentExposureKeys(Long userId, LocalDateTime from, LocalDateTime before) {
        if (userId == null) {
            return Set.of();
        }
        try {
            List<UserBehaviorLog> logs = mapper.selectList(new QueryWrapper<UserBehaviorLog>()
                    .select("target_type", "target_id")
                    .eq("user_id", userId)
                    .eq("behavior_type", RecommendationBehaviorType.EXPOSE.name())
                    .eq("status", ACTIVE)
                    .eq("is_deleted", NOT_DELETED)
                    .ge("event_time", from)
                    .le("event_time", before)
                    .last("LIMIT 5000"));
            Set<String> keys = new HashSet<>();
            if (logs != null) {
                for (UserBehaviorLog entry : logs) {
                    keys.add(entry.getTargetType() + "_" + entry.getTargetId());
                }
            }
            return keys;
        } catch (RuntimeException e) {
            log.warn("recent recommendation exposure query failed, fallback without penalty: {}", e.getMessage());
            return Set.of();
        }
    }

    public void recordInteractionSafely(Long userId, RecommendationBehaviorType type,
                                        RecommendationTargetType targetType, Long targetId) {
        if (userId == null || targetId == null) {
            return;
        }
        UserBehaviorLog logEntry = new UserBehaviorLog();
        logEntry.setClientEventId("server-" + UUID.randomUUID());
        logEntry.setUserId(userId);
        logEntry.setBehaviorType(type.name());
        logEntry.setTargetType(targetType.name());
        logEntry.setTargetId(targetId);
        logEntry.setScene("INTERACTION");
        logEntry.setEventTime(LocalDateTime.now());
        logEntry.setStatus(ACTIVE);
        logEntry.setIsDeleted(NOT_DELETED);
        try {
            mapper.insert(logEntry);
        } catch (RuntimeException e) {
            log.warn("recommendation interaction log failed without affecting business: {}", e.getMessage());
        }
    }

    private Set<String> existingClientEventIds(Set<String> ids) {
        if (ids.isEmpty()) {
            return Set.of();
        }
        List<UserBehaviorLog> existing = mapper.selectList(new QueryWrapper<UserBehaviorLog>()
                .select("client_event_id")
                .in("client_event_id", ids));
        Set<String> result = new HashSet<>();
        if (existing != null) {
            for (UserBehaviorLog entry : existing) {
                result.add(entry.getClientEventId());
            }
        }
        return result;
    }

    private boolean valid(RecommendationBehaviorBatchRequest.Event event) {
        if (event == null || event.getClientEventId() == null || event.getTargetId() == null
                || event.getTargetId() <= 0 || event.getEventTime() == null) {
            return false;
        }
        try {
            RecommendationBehaviorType.valueOf(event.getBehaviorType().toUpperCase(Locale.ROOT));
            RecommendationTargetType.valueOf(event.getTargetType().toUpperCase(Locale.ROOT));
        } catch (RuntimeException e) {
            return false;
        }
        if (event.getPosition() != null && event.getPosition() < 0) {
            return false;
        }
        return event.getDwellMs() == null || event.getDwellMs() >= 0;
    }

    private UserBehaviorLog toEntity(Long userId, RecommendationBehaviorBatchRequest.Event event) {
        UserBehaviorLog entity = new UserBehaviorLog();
        entity.setClientEventId(event.getClientEventId());
        entity.setUserId(userId);
        entity.setSessionId(event.getSessionId());
        entity.setBehaviorType(event.getBehaviorType().toUpperCase(Locale.ROOT));
        entity.setTargetType(event.getTargetType().toUpperCase(Locale.ROOT));
        entity.setTargetId(event.getTargetId());
        entity.setScene(event.getScene());
        entity.setAlgorithmVersion(event.getAlgorithmVersion());
        entity.setModelVersion(event.getModelVersion());
        entity.setExperimentId(event.getExperimentId());
        entity.setExperimentBucket(event.getExperimentBucket());
        entity.setRequestId(event.getRequestId());
        entity.setImpressionId(event.getImpressionId());
        entity.setPosition(event.getPosition());
        entity.setDwellMs(event.getDwellMs());
        entity.setEventTime(event.getEventTime());
        entity.setExtraJson(event.getExtraJson());
        entity.setStatus(ACTIVE);
        entity.setIsDeleted(NOT_DELETED);
        return entity;
    }
}

