package com.southstand.football.rank.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("football_standing")
public class FootballStanding {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long leagueId;
    private Long seasonId;
    private Long stageId;
    private String groupCode;
    private Long teamId;
    private Integer rankNo;
    private Integer played;
    private Integer won;
    private Integer drawn;
    private Integer lost;
    private Integer goalsFor;
    private Integer goalsAgainst;
    private Integer goalDifference;
    private Integer points;
    private Integer deductionPoints;
    private String formText;
    private String source;
    private LocalDateTime sourceUpdatedAt;
    private Integer isDeleted;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getLeagueId() { return leagueId; }
    public void setLeagueId(Long leagueId) { this.leagueId = leagueId; }
    public Long getSeasonId() { return seasonId; }
    public void setSeasonId(Long seasonId) { this.seasonId = seasonId; }
    public Long getStageId() { return stageId; }
    public void setStageId(Long stageId) { this.stageId = stageId; }
    public String getGroupCode() { return groupCode; }
    public void setGroupCode(String groupCode) { this.groupCode = groupCode; }
    public Long getTeamId() { return teamId; }
    public void setTeamId(Long teamId) { this.teamId = teamId; }
    public Integer getRankNo() { return rankNo; }
    public void setRankNo(Integer rankNo) { this.rankNo = rankNo; }
    public Integer getPlayed() { return played; }
    public void setPlayed(Integer played) { this.played = played; }
    public Integer getWon() { return won; }
    public void setWon(Integer won) { this.won = won; }
    public Integer getDrawn() { return drawn; }
    public void setDrawn(Integer drawn) { this.drawn = drawn; }
    public Integer getLost() { return lost; }
    public void setLost(Integer lost) { this.lost = lost; }
    public Integer getGoalsFor() { return goalsFor; }
    public void setGoalsFor(Integer goalsFor) { this.goalsFor = goalsFor; }
    public Integer getGoalsAgainst() { return goalsAgainst; }
    public void setGoalsAgainst(Integer goalsAgainst) { this.goalsAgainst = goalsAgainst; }
    public Integer getGoalDifference() { return goalDifference; }
    public void setGoalDifference(Integer goalDifference) { this.goalDifference = goalDifference; }
    public Integer getPoints() { return points; }
    public void setPoints(Integer points) { this.points = points; }
    public Integer getDeductionPoints() { return deductionPoints; }
    public void setDeductionPoints(Integer deductionPoints) { this.deductionPoints = deductionPoints; }
    public String getFormText() { return formText; }
    public void setFormText(String formText) { this.formText = formText; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public LocalDateTime getSourceUpdatedAt() { return sourceUpdatedAt; }
    public void setSourceUpdatedAt(LocalDateTime sourceUpdatedAt) { this.sourceUpdatedAt = sourceUpdatedAt; }
    public Integer getIsDeleted() { return isDeleted; }
    public void setIsDeleted(Integer isDeleted) { this.isDeleted = isDeleted; }
}
