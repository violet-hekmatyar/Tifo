package com.southstand.football.rank.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("football_player_competition_stat")
public class FootballPlayerCompetitionStat {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long leagueId;
    private Long seasonId;
    private Long stageId;
    private Long playerId;
    private Long teamId;
    private Integer appearances;
    private Integer starts;
    private Integer minutes;
    private Integer goals;
    private Integer assists;
    private Integer yellowCards;
    private Integer redCards;
    private Integer shots;
    private Integer shotsOnTarget;
    private Integer saves;
    private BigDecimal rating;
    private String source;
    private LocalDateTime sourceUpdatedAt;
    private Integer isDeleted;

    public Long getId() { return id; } public void setId(Long v) { id=v; }
    public Long getLeagueId() { return leagueId; } public void setLeagueId(Long v) { leagueId=v; }
    public Long getSeasonId() { return seasonId; } public void setSeasonId(Long v) { seasonId=v; }
    public Long getStageId() { return stageId; } public void setStageId(Long v) { stageId=v; }
    public Long getPlayerId() { return playerId; } public void setPlayerId(Long v) { playerId=v; }
    public Long getTeamId() { return teamId; } public void setTeamId(Long v) { teamId=v; }
    public Integer getAppearances() { return appearances; } public void setAppearances(Integer v) { appearances=v; }
    public Integer getStarts() { return starts; } public void setStarts(Integer v) { starts=v; }
    public Integer getMinutes() { return minutes; } public void setMinutes(Integer v) { minutes=v; }
    public Integer getGoals() { return goals; } public void setGoals(Integer v) { goals=v; }
    public Integer getAssists() { return assists; } public void setAssists(Integer v) { assists=v; }
    public Integer getYellowCards() { return yellowCards; } public void setYellowCards(Integer v) { yellowCards=v; }
    public Integer getRedCards() { return redCards; } public void setRedCards(Integer v) { redCards=v; }
    public Integer getShots() { return shots; } public void setShots(Integer v) { shots=v; }
    public Integer getShotsOnTarget() { return shotsOnTarget; } public void setShotsOnTarget(Integer v) { shotsOnTarget=v; }
    public Integer getSaves() { return saves; } public void setSaves(Integer v) { saves=v; }
    public BigDecimal getRating() { return rating; } public void setRating(BigDecimal v) { rating=v; }
    public String getSource() { return source; } public void setSource(String v) { source=v; }
    public LocalDateTime getSourceUpdatedAt() { return sourceUpdatedAt; } public void setSourceUpdatedAt(LocalDateTime v) { sourceUpdatedAt=v; }
    public Integer getIsDeleted() { return isDeleted; } public void setIsDeleted(Integer v) { isDeleted=v; }
}
