package com.southstand.user.vo;

import java.time.LocalDateTime;

public class UserFollowItemVO {

    private Long userId;
    private String username;
    private String nickname;
    private String avatarUrl;
    private String bio;
    private UserRelationStatus relationStatus;
    private LocalDateTime followedAt;

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }
    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }
    public UserRelationStatus getRelationStatus() { return relationStatus; }
    public void setRelationStatus(UserRelationStatus relationStatus) { this.relationStatus = relationStatus; }
    public LocalDateTime getFollowedAt() { return followedAt; }
    public void setFollowedAt(LocalDateTime followedAt) { this.followedAt = followedAt; }
}
