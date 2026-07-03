package com.southstand.card.vo;

public class HotLeagueVO {

    private Long leagueId;
    private String leagueName;
    private String logoUrl;
    private String country;
    private Double hotScore;
    private Integer liveMatchCount;
    private Integer upcomingMatchCount;

    public Long getLeagueId() { return leagueId; }
    public void setLeagueId(Long leagueId) { this.leagueId = leagueId; }
    public String getLeagueName() { return leagueName; }
    public void setLeagueName(String leagueName) { this.leagueName = leagueName; }
    public String getLogoUrl() { return logoUrl; }
    public void setLogoUrl(String logoUrl) { this.logoUrl = logoUrl; }
    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }
    public Double getHotScore() { return hotScore; }
    public void setHotScore(Double hotScore) { this.hotScore = hotScore; }
    public Integer getLiveMatchCount() { return liveMatchCount; }
    public void setLiveMatchCount(Integer liveMatchCount) { this.liveMatchCount = liveMatchCount; }
    public Integer getUpcomingMatchCount() { return upcomingMatchCount; }
    public void setUpcomingMatchCount(Integer upcomingMatchCount) { this.upcomingMatchCount = upcomingMatchCount; }
}
