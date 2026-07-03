package com.southstand.football.league.vo;

public class LeagueVO {

    private Long leagueId;
    private String leagueName;
    private String leagueNameEn;
    private String country;
    private String logoUrl;
    private String season;
    private String leagueType;

    public Long getLeagueId() { return leagueId; }
    public void setLeagueId(Long leagueId) { this.leagueId = leagueId; }
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
}
