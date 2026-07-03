package com.southstand.content.vo;

public class AuthorVO {
    private Long userId;
    private String nickname;
    private String avatarUrl;
    private Boolean verified;

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }
    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
    public Boolean getVerified() { return verified; }
    public void setVerified(Boolean verified) { this.verified = verified; }
}
