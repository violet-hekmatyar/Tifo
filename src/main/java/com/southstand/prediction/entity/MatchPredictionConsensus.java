package com.southstand.prediction.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("match_prediction_consensus")
public class MatchPredictionConsensus {

    @TableId
    private Long matchId;
    private BigDecimal homePercent;
    private BigDecimal drawPercent;
    private BigDecimal awayPercent;
    private Integer participantCount;
    private LocalDateTime updatedAt;

    public Long getMatchId() { return matchId; }
    public void setMatchId(Long matchId) { this.matchId = matchId; }
    public BigDecimal getHomePercent() { return homePercent; }
    public void setHomePercent(BigDecimal homePercent) { this.homePercent = homePercent; }
    public BigDecimal getDrawPercent() { return drawPercent; }
    public void setDrawPercent(BigDecimal drawPercent) { this.drawPercent = drawPercent; }
    public BigDecimal getAwayPercent() { return awayPercent; }
    public void setAwayPercent(BigDecimal awayPercent) { this.awayPercent = awayPercent; }
    public Integer getParticipantCount() { return participantCount; }
    public void setParticipantCount(Integer participantCount) { this.participantCount = participantCount; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
