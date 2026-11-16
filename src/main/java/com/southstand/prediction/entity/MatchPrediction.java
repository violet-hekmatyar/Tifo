package com.southstand.prediction.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("match_prediction")
public class MatchPrediction {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long matchId;
    private Long userId;
    private Integer homeProbability;
    private Integer drawProbability;
    private Integer awayProbability;
    private String predictedOutcome;
    private Integer insightScore;
    private Integer stake;
    private String challengeResult;
    private Integer rewardBeans;
    private String status;
    private LocalDateTime submittedAt;
    private LocalDateTime settledAt;
    private String extraJson;
    private String remark;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getMatchId() { return matchId; }
    public void setMatchId(Long matchId) { this.matchId = matchId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Integer getHomeProbability() { return homeProbability; }
    public void setHomeProbability(Integer homeProbability) { this.homeProbability = homeProbability; }
    public Integer getDrawProbability() { return drawProbability; }
    public void setDrawProbability(Integer drawProbability) { this.drawProbability = drawProbability; }
    public Integer getAwayProbability() { return awayProbability; }
    public void setAwayProbability(Integer awayProbability) { this.awayProbability = awayProbability; }
    public String getPredictedOutcome() { return predictedOutcome; }
    public void setPredictedOutcome(String predictedOutcome) { this.predictedOutcome = predictedOutcome; }
    public Integer getInsightScore() { return insightScore; }
    public void setInsightScore(Integer insightScore) { this.insightScore = insightScore; }
    public Integer getStake() { return stake; }
    public void setStake(Integer stake) { this.stake = stake; }
    public String getChallengeResult() { return challengeResult; }
    public void setChallengeResult(String challengeResult) { this.challengeResult = challengeResult; }
    public Integer getRewardBeans() { return rewardBeans; }
    public void setRewardBeans(Integer rewardBeans) { this.rewardBeans = rewardBeans; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(LocalDateTime submittedAt) { this.submittedAt = submittedAt; }
    public LocalDateTime getSettledAt() { return settledAt; }
    public void setSettledAt(LocalDateTime settledAt) { this.settledAt = settledAt; }
    public String getExtraJson() { return extraJson; }
    public void setExtraJson(String extraJson) { this.extraJson = extraJson; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
}
