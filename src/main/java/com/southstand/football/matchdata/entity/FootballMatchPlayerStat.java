package com.southstand.football.matchdata.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;

@TableName("football_match_player_stat")
public class FootballMatchPlayerStat {
    @TableId(type=IdType.ASSIGN_ID) private Long id;
    private Long matchId; private Long teamId; private Long playerId; private Integer minutes; private Integer goals; private Integer assists; private Integer shots; private Integer shotsOnTarget;
    private Integer passes; private Integer successfulPasses; private Integer keyPasses; private Integer tackles; private Integer interceptions; private Integer saves; private Integer yellowCards; private Integer redCards;
    private BigDecimal officialRating; private String source; private Integer isDeleted;
    public Long getId(){return id;} public void setId(Long v){id=v;} public Long getMatchId(){return matchId;} public void setMatchId(Long v){matchId=v;} public Long getTeamId(){return teamId;} public void setTeamId(Long v){teamId=v;} public Long getPlayerId(){return playerId;} public void setPlayerId(Long v){playerId=v;}
    public Integer getMinutes(){return minutes;} public void setMinutes(Integer v){minutes=v;} public Integer getGoals(){return goals;} public void setGoals(Integer v){goals=v;} public Integer getAssists(){return assists;} public void setAssists(Integer v){assists=v;}
    public Integer getShots(){return shots;} public void setShots(Integer v){shots=v;} public Integer getShotsOnTarget(){return shotsOnTarget;} public void setShotsOnTarget(Integer v){shotsOnTarget=v;} public Integer getPasses(){return passes;} public void setPasses(Integer v){passes=v;} public Integer getSuccessfulPasses(){return successfulPasses;} public void setSuccessfulPasses(Integer v){successfulPasses=v;}
    public Integer getKeyPasses(){return keyPasses;} public void setKeyPasses(Integer v){keyPasses=v;} public Integer getTackles(){return tackles;} public void setTackles(Integer v){tackles=v;} public Integer getInterceptions(){return interceptions;} public void setInterceptions(Integer v){interceptions=v;} public Integer getSaves(){return saves;} public void setSaves(Integer v){saves=v;}
    public Integer getYellowCards(){return yellowCards;} public void setYellowCards(Integer v){yellowCards=v;} public Integer getRedCards(){return redCards;} public void setRedCards(Integer v){redCards=v;} public BigDecimal getOfficialRating(){return officialRating;} public void setOfficialRating(BigDecimal v){officialRating=v;} public String getSource(){return source;} public void setSource(String v){source=v;} public Integer getIsDeleted(){return isDeleted;} public void setIsDeleted(Integer v){isDeleted=v;}
}
