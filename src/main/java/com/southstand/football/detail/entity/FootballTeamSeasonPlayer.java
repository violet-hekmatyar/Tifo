package com.southstand.football.detail.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDate;

@TableName("football_team_season_player")
public class FootballTeamSeasonPlayer {
    @TableId(type=IdType.ASSIGN_ID) private Long id;
    private Long leagueId; private Long seasonId; private Long teamId; private Long playerId;
    private String position; private Integer shirtNumber; private String squadRole;
    private Integer captainFlag; private Integer loanFlag; private Long loanFromTeamId;
    private LocalDate joinedDate; private LocalDate leftDate; private String status; private String source; private Integer isDeleted;
    public Long getId(){return id;} public void setId(Long v){id=v;}
    public Long getLeagueId(){return leagueId;} public void setLeagueId(Long v){leagueId=v;}
    public Long getSeasonId(){return seasonId;} public void setSeasonId(Long v){seasonId=v;}
    public Long getTeamId(){return teamId;} public void setTeamId(Long v){teamId=v;}
    public Long getPlayerId(){return playerId;} public void setPlayerId(Long v){playerId=v;}
    public String getPosition(){return position;} public void setPosition(String v){position=v;}
    public Integer getShirtNumber(){return shirtNumber;} public void setShirtNumber(Integer v){shirtNumber=v;}
    public String getSquadRole(){return squadRole;} public void setSquadRole(String v){squadRole=v;}
    public Integer getCaptainFlag(){return captainFlag;} public void setCaptainFlag(Integer v){captainFlag=v;}
    public Integer getLoanFlag(){return loanFlag;} public void setLoanFlag(Integer v){loanFlag=v;}
    public Long getLoanFromTeamId(){return loanFromTeamId;} public void setLoanFromTeamId(Long v){loanFromTeamId=v;}
    public LocalDate getJoinedDate(){return joinedDate;} public void setJoinedDate(LocalDate v){joinedDate=v;}
    public LocalDate getLeftDate(){return leftDate;} public void setLeftDate(LocalDate v){leftDate=v;}
    public String getStatus(){return status;} public void setStatus(String v){status=v;}
    public String getSource(){return source;} public void setSource(String v){source=v;}
    public Integer getIsDeleted(){return isDeleted;} public void setIsDeleted(Integer v){isDeleted=v;}
}
