package com.southstand.football.matchdata.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;

@TableName("football_match_player_appearance")
public class FootballMatchPlayerAppearance {
    @TableId(type=IdType.ASSIGN_ID) private Long id;
    private Long matchId; private Long teamId; private Long playerId; private String lineupType; private String position; private Integer shirtNumber;
    private Integer captainFlag; private Integer startedFlag; private Integer appearedFlag; private Integer startMinute; private Integer endMinute;
    private Integer substitutedInMinute; private Integer substitutedOutMinute; private BigDecimal fieldX; private BigDecimal fieldY; private String status; private String source; private Integer isDeleted;
    public Long getId(){return id;} public void setId(Long v){id=v;} public Long getMatchId(){return matchId;} public void setMatchId(Long v){matchId=v;}
    public Long getTeamId(){return teamId;} public void setTeamId(Long v){teamId=v;} public Long getPlayerId(){return playerId;} public void setPlayerId(Long v){playerId=v;}
    public String getLineupType(){return lineupType;} public void setLineupType(String v){lineupType=v;} public String getPosition(){return position;} public void setPosition(String v){position=v;}
    public Integer getShirtNumber(){return shirtNumber;} public void setShirtNumber(Integer v){shirtNumber=v;} public Integer getCaptainFlag(){return captainFlag;} public void setCaptainFlag(Integer v){captainFlag=v;}
    public Integer getStartedFlag(){return startedFlag;} public void setStartedFlag(Integer v){startedFlag=v;} public Integer getAppearedFlag(){return appearedFlag;} public void setAppearedFlag(Integer v){appearedFlag=v;}
    public Integer getStartMinute(){return startMinute;} public void setStartMinute(Integer v){startMinute=v;} public Integer getEndMinute(){return endMinute;} public void setEndMinute(Integer v){endMinute=v;}
    public Integer getSubstitutedInMinute(){return substitutedInMinute;} public void setSubstitutedInMinute(Integer v){substitutedInMinute=v;} public Integer getSubstitutedOutMinute(){return substitutedOutMinute;} public void setSubstitutedOutMinute(Integer v){substitutedOutMinute=v;}
    public BigDecimal getFieldX(){return fieldX;} public void setFieldX(BigDecimal v){fieldX=v;} public BigDecimal getFieldY(){return fieldY;} public void setFieldY(BigDecimal v){fieldY=v;}
    public String getStatus(){return status;} public void setStatus(String v){status=v;} public String getSource(){return source;} public void setSource(String v){source=v;} public Integer getIsDeleted(){return isDeleted;} public void setIsDeleted(Integer v){isDeleted=v;}
}
