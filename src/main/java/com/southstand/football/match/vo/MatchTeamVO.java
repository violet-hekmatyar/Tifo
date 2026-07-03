package com.southstand.football.match.vo;

public class MatchTeamVO {

    private Long teamId;
    private String teamName;
    private String logoUrl;
    private Integer score;

    public Long getTeamId() { return teamId; }
    public void setTeamId(Long teamId) { this.teamId = teamId; }
    public String getTeamName() { return teamName; }
    public void setTeamName(String teamName) { this.teamName = teamName; }
    public String getLogoUrl() { return logoUrl; }
    public void setLogoUrl(String logoUrl) { this.logoUrl = logoUrl; }
    public Integer getScore() { return score; }
    public void setScore(Integer score) { this.score = score; }
}
