package com.southstand.football.team.vo;

import com.southstand.football.match.vo.MatchListVO;
import java.util.List;

public class TeamDetailVO {

    private Long teamId;
    private String teamName;
    private String teamNameEn;
    private String shortName;
    private String logoUrl;
    private String country;
    private String city;
    private String stadiumName;
    private Integer foundedYear;
    private String coachName;
    private String marketValue;
    private Integer followerCount;
    private Boolean followed;
    private List<MatchListVO> recentMatches;
    private List<MatchListVO> upcomingMatches;

    public Long getTeamId() { return teamId; }
    public void setTeamId(Long teamId) { this.teamId = teamId; }
    public String getTeamName() { return teamName; }
    public void setTeamName(String teamName) { this.teamName = teamName; }
    public String getTeamNameEn() { return teamNameEn; }
    public void setTeamNameEn(String teamNameEn) { this.teamNameEn = teamNameEn; }
    public String getShortName() { return shortName; }
    public void setShortName(String shortName) { this.shortName = shortName; }
    public String getLogoUrl() { return logoUrl; }
    public void setLogoUrl(String logoUrl) { this.logoUrl = logoUrl; }
    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    public String getStadiumName() { return stadiumName; }
    public void setStadiumName(String stadiumName) { this.stadiumName = stadiumName; }
    public Integer getFoundedYear() { return foundedYear; }
    public void setFoundedYear(Integer foundedYear) { this.foundedYear = foundedYear; }
    public String getCoachName() { return coachName; }
    public void setCoachName(String coachName) { this.coachName = coachName; }
    public String getMarketValue() { return marketValue; }
    public void setMarketValue(String marketValue) { this.marketValue = marketValue; }
    public Integer getFollowerCount() { return followerCount; }
    public void setFollowerCount(Integer followerCount) { this.followerCount = followerCount; }
    public Boolean getFollowed() { return followed; }
    public void setFollowed(Boolean followed) { this.followed = followed; }
    public List<MatchListVO> getRecentMatches() { return recentMatches; }
    public void setRecentMatches(List<MatchListVO> recentMatches) { this.recentMatches = recentMatches; }
    public List<MatchListVO> getUpcomingMatches() { return upcomingMatches; }
    public void setUpcomingMatches(List<MatchListVO> upcomingMatches) { this.upcomingMatches = upcomingMatches; }
}
