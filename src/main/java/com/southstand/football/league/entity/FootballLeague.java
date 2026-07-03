package com.southstand.football.league.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

@TableName("football_league")
public class FootballLeague {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private String leagueName;
    private String leagueNameEn;
    private String country;
    private String logoUrl;
    private String season;
    private String leagueType;
    private Integer sortOrder;
    private String status;
    private Integer isDeleted;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getLeagueName() { return leagueName; }
    public void setLeagueName(String leagueName) { this.leagueName = leagueName; }
    public String getLeagueNameEn() { return leagueNameEn; }
    public void setLeagueNameEn(String leagueNameEn) { this.leagueNameEn = leagueNameEn; }
    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }
    public String getLogoUrl() { return logoUrl; }
    public void setLogoUrl(String logoUrl) { this.logoUrl = logoUrl; }
    public String getSeason() { return season; }
    public void setSeason(String season) { this.season = season; }
    public String getLeagueType() { return leagueType; }
    public void setLeagueType(String leagueType) { this.leagueType = leagueType; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Integer getIsDeleted() { return isDeleted; }
    public void setIsDeleted(Integer isDeleted) { this.isDeleted = isDeleted; }
}
