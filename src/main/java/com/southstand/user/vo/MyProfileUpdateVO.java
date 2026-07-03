package com.southstand.user.vo;

public class MyProfileUpdateVO {

    private Long userId;
    private String nickname;
    private String avatarUrl;
    private String bio;
    private Long mainTeamId;

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }
    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }
    public Long getMainTeamId() { return mainTeamId; }
    public void setMainTeamId(Long mainTeamId) { this.mainTeamId = mainTeamId; }
}
