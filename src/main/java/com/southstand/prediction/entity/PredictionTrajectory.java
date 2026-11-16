package com.southstand.prediction.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("prediction_trajectory")
public class PredictionTrajectory {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long matchId;
    private Long userId;
    private String stageLabel;
    private Integer homePercent;
    private Integer drawPercent;
    private Integer awayPercent;
    private Integer sortOrder;
    private LocalDateTime capturedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getMatchId() { return matchId; }
    public void setMatchId(Long matchId) { this.matchId = matchId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getStageLabel() { return stageLabel; }
    public void setStageLabel(String stageLabel) { this.stageLabel = stageLabel; }
    public Integer getHomePercent() { return homePercent; }
    public void setHomePercent(Integer homePercent) { this.homePercent = homePercent; }
    public Integer getDrawPercent() { return drawPercent; }
    public void setDrawPercent(Integer drawPercent) { this.drawPercent = drawPercent; }
    public Integer getAwayPercent() { return awayPercent; }
    public void setAwayPercent(Integer awayPercent) { this.awayPercent = awayPercent; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
    public LocalDateTime getCapturedAt() { return capturedAt; }
    public void setCapturedAt(LocalDateTime capturedAt) { this.capturedAt = capturedAt; }
}
