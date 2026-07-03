package com.southstand.football.event.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

@TableName("match_event")
public class MatchEvent {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long matchId;
    private Long teamId;
    private Long playerId;
    private Long assistPlayerId;
    private String eventType;
    private Integer minute;
    private Integer extraMinute;
    private String scoreAfter;
    private String description;
    private Integer hasDebate;
    private String status;
    private Integer isDeleted;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getMatchId() { return matchId; }
    public void setMatchId(Long matchId) { this.matchId = matchId; }
    public Long getTeamId() { return teamId; }
    public void setTeamId(Long teamId) { this.teamId = teamId; }
    public Long getPlayerId() { return playerId; }
    public void setPlayerId(Long playerId) { this.playerId = playerId; }
    public Long getAssistPlayerId() { return assistPlayerId; }
    public void setAssistPlayerId(Long assistPlayerId) { this.assistPlayerId = assistPlayerId; }
    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    public Integer getMinute() { return minute; }
    public void setMinute(Integer minute) { this.minute = minute; }
    public Integer getExtraMinute() { return extraMinute; }
    public void setExtraMinute(Integer extraMinute) { this.extraMinute = extraMinute; }
    public String getScoreAfter() { return scoreAfter; }
    public void setScoreAfter(String scoreAfter) { this.scoreAfter = scoreAfter; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Integer getHasDebate() { return hasDebate; }
    public void setHasDebate(Integer hasDebate) { this.hasDebate = hasDebate; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Integer getIsDeleted() { return isDeleted; }
    public void setIsDeleted(Integer isDeleted) { this.isDeleted = isDeleted; }
}
