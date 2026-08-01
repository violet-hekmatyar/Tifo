package com.southstand.football.rank.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("football_team_competition_stat")
public class FootballTeamCompetitionStat {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long leagueId;
    private Long seasonId;
    private Long stageId;
    private Long teamId;
    private Integer played;
    private Integer goalsFor;
    private Integer goalsAgainst;
    private Integer assists;
    private Integer yellowCards;
    private Integer redCards;
    private Integer shots;
    private Integer shotsOnTarget;
    private Integer corners;
    private Integer fouls;
    private Integer cleanSheets;
    private BigDecimal avgRating;
    private String source;
    private LocalDateTime sourceUpdatedAt;
    private Integer isDeleted;

    public Long getId() { return id; } public void setId(Long v) { id=v; }
    public Long getLeagueId() { return leagueId; } public void setLeagueId(Long v) { leagueId=v; }
    public Long getSeasonId() { return seasonId; } public void setSeasonId(Long v) { seasonId=v; }
    public Long getStageId() { return stageId; } public void setStageId(Long v) { stageId=v; }
    public Long getTeamId() { return teamId; } public void setTeamId(Long v) { teamId=v; }
    public Integer getPlayed() { return played; } public void setPlayed(Integer v) { played=v; }
    public Integer getGoalsFor() { return goalsFor; } public void setGoalsFor(Integer v) { goalsFor=v; }
    public Integer getGoalsAgainst() { return goalsAgainst; } public void setGoalsAgainst(Integer v) { goalsAgainst=v; }
    public Integer getAssists() { return assists; } public void setAssists(Integer v) { assists=v; }
    public Integer getYellowCards() { return yellowCards; } public void setYellowCards(Integer v) { yellowCards=v; }
    public Integer getRedCards() { return redCards; } public void setRedCards(Integer v) { redCards=v; }
    public Integer getShots() { return shots; } public void setShots(Integer v) { shots=v; }
    public Integer getShotsOnTarget() { return shotsOnTarget; } public void setShotsOnTarget(Integer v) { shotsOnTarget=v; }
    public Integer getCorners() { return corners; } public void setCorners(Integer v) { corners=v; }
    public Integer getFouls() { return fouls; } public void setFouls(Integer v) { fouls=v; }
    public Integer getCleanSheets() { return cleanSheets; } public void setCleanSheets(Integer v) { cleanSheets=v; }
    public BigDecimal getAvgRating() { return avgRating; } public void setAvgRating(BigDecimal v) { avgRating=v; }
    public String getSource() { return source; } public void setSource(String v) { source=v; }
    public LocalDateTime getSourceUpdatedAt() { return sourceUpdatedAt; } public void setSourceUpdatedAt(LocalDateTime v) { sourceUpdatedAt=v; }
    public Integer getIsDeleted() { return isDeleted; } public void setIsDeleted(Integer v) { isDeleted=v; }
}
