package com.southstand.recommend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("user_behavior_log")
public class UserBehaviorLog {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private String clientEventId;
    private Long userId;
    private String sessionId;
    private String behaviorType;
    private String targetType;
    private Long targetId;
    private String scene;
    private String algorithmVersion;
    private String modelVersion;
    private String experimentId;
    private String experimentBucket;
    private String requestId;
    private String impressionId;
    private Integer position;
    private Long dwellMs;
    private LocalDateTime eventTime;
    private String extraJson;
    private String status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Integer isDeleted;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getClientEventId() { return clientEventId; }
    public void setClientEventId(String clientEventId) { this.clientEventId = clientEventId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
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
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
    public Integer getIsDeleted() { return isDeleted; }
    public void setIsDeleted(Integer isDeleted) { this.isDeleted = isDeleted; }
}

