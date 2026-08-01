package com.southstand.football.detail.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDate;

@TableName("football_player_team_history")
public class FootballPlayerTeamHistory {
    @TableId(type=IdType.ASSIGN_ID) private Long id;
    private Long playerId; private Long teamId; private Long seasonId; private LocalDate startDate; private LocalDate endDate;
    private Integer shirtNumber; private String position; private Integer appearances; private Integer goals; private Integer assists;
    private Integer currentFlag; private Integer loanFlag; private String source; private Integer isDeleted;
    public Long getId(){return id;} public void setId(Long v){id=v;}
    public Long getPlayerId(){return playerId;} public void setPlayerId(Long v){playerId=v;}
    public Long getTeamId(){return teamId;} public void setTeamId(Long v){teamId=v;}
    public Long getSeasonId(){return seasonId;} public void setSeasonId(Long v){seasonId=v;}
    public LocalDate getStartDate(){return startDate;} public void setStartDate(LocalDate v){startDate=v;}
    public LocalDate getEndDate(){return endDate;} public void setEndDate(LocalDate v){endDate=v;}
    public Integer getShirtNumber(){return shirtNumber;} public void setShirtNumber(Integer v){shirtNumber=v;}
    public String getPosition(){return position;} public void setPosition(String v){position=v;}
    public Integer getAppearances(){return appearances;} public void setAppearances(Integer v){appearances=v;}
    public Integer getGoals(){return goals;} public void setGoals(Integer v){goals=v;}
    public Integer getAssists(){return assists;} public void setAssists(Integer v){assists=v;}
    public Integer getCurrentFlag(){return currentFlag;} public void setCurrentFlag(Integer v){currentFlag=v;}
    public Integer getLoanFlag(){return loanFlag;} public void setLoanFlag(Integer v){loanFlag=v;}
    public String getSource(){return source;} public void setSource(String v){source=v;}
    public Integer getIsDeleted(){return isDeleted;} public void setIsDeleted(Integer v){isDeleted=v;}
}
