package com.southstand.football.player.vo;

public class PlayerTeamVO {

    private Long teamId;
    private String teamName;
    private String logoUrl;
    private Integer shirtNumber;
    private String position;

    public Long getTeamId() { return teamId; }
    public void setTeamId(Long teamId) { this.teamId = teamId; }
    public String getTeamName() { return teamName; }
    public void setTeamName(String teamName) { this.teamName = teamName; }
    public String getLogoUrl() { return logoUrl; }
    public void setLogoUrl(String logoUrl) { this.logoUrl = logoUrl; }
    public Integer getShirtNumber() { return shirtNumber; }
    public void setShirtNumber(Integer shirtNumber) { this.shirtNumber = shirtNumber; }
    public String getPosition() { return position; }
    public void setPosition(String position) { this.position = position; }
}
