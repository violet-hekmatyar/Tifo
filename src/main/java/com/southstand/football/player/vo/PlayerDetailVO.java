package com.southstand.football.player.vo;

public class PlayerDetailVO {

    private Long playerId;
    private String playerName;
    private String playerNameEn;
    private String avatarUrl;
    private String position;
    private String nationality;
    private Integer age;
    private Integer shirtNumber;
    private Boolean retired;
    private Integer followerCount;
    private PlayerTeamVO team;
    private Boolean followed;

    public Long getPlayerId() { return playerId; }
    public void setPlayerId(Long playerId) { this.playerId = playerId; }
    public String getPlayerName() { return playerName; }
    public void setPlayerName(String playerName) { this.playerName = playerName; }
    public String getPlayerNameEn() { return playerNameEn; }
    public void setPlayerNameEn(String playerNameEn) { this.playerNameEn = playerNameEn; }
    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
    public String getPosition() { return position; }
    public void setPosition(String position) { this.position = position; }
    public String getNationality() { return nationality; }
    public void setNationality(String nationality) { this.nationality = nationality; }
    public Integer getAge() { return age; }
    public void setAge(Integer age) { this.age = age; }
    public Integer getShirtNumber() { return shirtNumber; }
    public void setShirtNumber(Integer shirtNumber) { this.shirtNumber = shirtNumber; }
    public Boolean getRetired() { return retired; }
    public void setRetired(Boolean retired) { this.retired = retired; }
    public Integer getFollowerCount() { return followerCount; }
    public void setFollowerCount(Integer followerCount) { this.followerCount = followerCount; }
    public PlayerTeamVO getTeam() { return team; }
    public void setTeam(PlayerTeamVO team) { this.team = team; }
    public Boolean getFollowed() { return followed; }
    public void setFollowed(Boolean followed) { this.followed = followed; }
}
