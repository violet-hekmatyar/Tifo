package com.southstand.football.matchdata.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

@TableName("football_match_lineup")
public class FootballMatchLineup {
    @TableId(type=IdType.ASSIGN_ID) private Long id;
    private Long matchId; private Long teamId; private String formation; private String coachName; private Integer confirmedFlag; private String source; private Integer isDeleted;
    public Long getId(){return id;} public void setId(Long v){id=v;} public Long getMatchId(){return matchId;} public void setMatchId(Long v){matchId=v;}
    public Long getTeamId(){return teamId;} public void setTeamId(Long v){teamId=v;} public String getFormation(){return formation;} public void setFormation(String v){formation=v;}
    public String getCoachName(){return coachName;} public void setCoachName(String v){coachName=v;} public Integer getConfirmedFlag(){return confirmedFlag;} public void setConfirmedFlag(Integer v){confirmedFlag=v;}
    public String getSource(){return source;} public void setSource(String v){source=v;} public Integer getIsDeleted(){return isDeleted;} public void setIsDeleted(Integer v){isDeleted=v;}
}
