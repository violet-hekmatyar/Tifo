package com.southstand.recommend.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;

public class RecommendationBehaviorBatchRequest {

    @NotEmpty
    @Size(max = 100)
    private List<@Valid Event> events;

    public List<Event> getEvents() { return events; }
    public void setEvents(List<Event> events) { this.events = events; }

    public static class Event {
        @NotBlank @Size(max = 64)
        private String clientEventId;
        @Size(max = 64)
        private String sessionId;
        @NotBlank @Size(max = 32)
        private String behaviorType;
        @NotBlank @Size(max = 32)
        private String targetType;
        @NotNull
        private Long targetId;
        @Size(max = 32)
        private String scene;
        @Size(max = 32)
        private String algorithmVersion;
        @Size(max = 64)
        private String modelVersion;
        @Size(max = 64)
        private String experimentId;
        @Size(max = 32)
        private String experimentBucket;
        @Size(max = 64)
        private String requestId;
        @Size(max = 96)
        private String impressionId;
        private Integer position;
        private Long dwellMs;
        @NotNull
        private LocalDateTime eventTime;
        @Size(max = 1000)
        private String extraJson;

        public String getClientEventId() { return clientEventId; }
        public void setClientEventId(String clientEventId) { this.clientEventId = clientEventId; }
        public String getSessionId() { return sessionId; }
        public void setSessionId(String sessionId) { this.sessionId = sessionId; }
        public String getBehaviorType() { return behaviorType; }
        public void setBehaviorType(String behaviorType) { this.behaviorType = behaviorType; }
        public String getTargetType() { return targetType; }
        public void setTargetType(String targetType) { this.targetType = targetType; }
        public Long getTargetId() { return targetId; }
        public void setTargetId(Long targetId) { this.targetId = targetId; }
        public String getScene() { return scene; }
        public void setScene(String scene) { this.scene = scene; }
        public String getAlgorithmVersion() { return algorithmVersion; }
        public void setAlgorithmVersion(String algorithmVersion) { this.algorithmVersion = algorithmVersion; }
        public String getModelVersion() { return modelVersion; }
        public void setModelVersion(String modelVersion) { this.modelVersion = modelVersion; }
        public String getExperimentId() { return experimentId; }
        public void setExperimentId(String experimentId) { this.experimentId = experimentId; }
        public String getExperimentBucket() { return experimentBucket; }
        public void setExperimentBucket(String experimentBucket) { this.experimentBucket = experimentBucket; }
        public String getRequestId() { return requestId; }
        public void setRequestId(String requestId) { this.requestId = requestId; }
        public String getImpressionId() { return impressionId; }
        public void setImpressionId(String impressionId) { this.impressionId = impressionId; }
        public Integer getPosition() { return position; }
        public void setPosition(Integer position) { this.position = position; }
        public Long getDwellMs() { return dwellMs; }
        public void setDwellMs(Long dwellMs) { this.dwellMs = dwellMs; }
        public LocalDateTime getEventTime() { return eventTime; }
        public void setEventTime(LocalDateTime eventTime) { this.eventTime = eventTime; }
        public String getExtraJson() { return extraJson; }
        public void setExtraJson(String extraJson) { this.extraJson = extraJson; }
    }
}

