package com.southstand.football.detail.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

@TableName("football_team_honor")
public class FootballTeamHonor {
    @TableId(type=IdType.ASSIGN_ID) private Long id;
    private Long teamId; private Long leagueId; private String honorName; private String honorType;
    private Integer titleCount; private String winningYears; private Integer latestYear; private String source; private Integer isDeleted;
    public Long getId(){return id;} public void setId(Long v){id=v;}
    public Long getTeamId(){return teamId;} public void setTeamId(Long v){teamId=v;}
    public Long getLeagueId(){return leagueId;} public void setLeagueId(Long v){leagueId=v;}
    public String getHonorName(){return honorName;} public void setHonorName(String v){honorName=v;}
    public String getHonorType(){return honorType;} public void setHonorType(String v){honorType=v;}
    public Integer getTitleCount(){return titleCount;} public void setTitleCount(Integer v){titleCount=v;}
    public String getWinningYears(){return winningYears;} public void setWinningYears(String v){winningYears=v;}
    public Integer getLatestYear(){return latestYear;} public void setLatestYear(Integer v){latestYear=v;}
    public String getSource(){return source;} public void setSource(String v){source=v;}
    public Integer getIsDeleted(){return isDeleted;} public void setIsDeleted(Integer v){isDeleted=v;}
}
