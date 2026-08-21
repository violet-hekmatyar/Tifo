package com.southstand.football.matchdata.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;

@TableName("football_match_team_stat")
public class FootballMatchTeamStat {
    @TableId(type=IdType.ASSIGN_ID) private Long id;
    private Long matchId; private Long teamId; private BigDecimal possession; private Integer shots; private Integer shotsOnTarget; private Integer corners;
    private Integer fouls; private Integer offsides; private Integer yellowCards; private Integer redCards; private Integer passes; private Integer successfulPasses;
    private BigDecimal passAccuracy; private Integer saves; private BigDecimal expectedGoals; private String source; private Integer isDeleted;
    public Long getId(){return id;} public void setId(Long v){id=v;} public Long getMatchId(){return matchId;} public void setMatchId(Long v){matchId=v;} public Long getTeamId(){return teamId;} public void setTeamId(Long v){teamId=v;}
    public BigDecimal getPossession(){return possession;} public void setPossession(BigDecimal v){possession=v;} public Integer getShots(){return shots;} public void setShots(Integer v){shots=v;} public Integer getShotsOnTarget(){return shotsOnTarget;} public void setShotsOnTarget(Integer v){shotsOnTarget=v;}
    public Integer getCorners(){return corners;} public void setCorners(Integer v){corners=v;} public Integer getFouls(){return fouls;} public void setFouls(Integer v){fouls=v;} public Integer getOffsides(){return offsides;} public void setOffsides(Integer v){offsides=v;}
    public Integer getYellowCards(){return yellowCards;} public void setYellowCards(Integer v){yellowCards=v;} public Integer getRedCards(){return redCards;} public void setRedCards(Integer v){redCards=v;} public Integer getPasses(){return passes;} public void setPasses(Integer v){passes=v;}
    public Integer getSuccessfulPasses(){return successfulPasses;} public void setSuccessfulPasses(Integer v){successfulPasses=v;} public BigDecimal getPassAccuracy(){return passAccuracy;} public void setPassAccuracy(BigDecimal v){passAccuracy=v;}
    public Integer getSaves(){return saves;} public void setSaves(Integer v){saves=v;} public BigDecimal getExpectedGoals(){return expectedGoals;} public void setExpectedGoals(BigDecimal v){expectedGoals=v;} public String getSource(){return source;} public void setSource(String v){source=v;} public Integer getIsDeleted(){return isDeleted;} public void setIsDeleted(Integer v){isDeleted=v;}
}
