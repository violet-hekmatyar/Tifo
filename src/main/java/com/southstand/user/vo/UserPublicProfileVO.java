package com.southstand.user.vo;

public class UserPublicProfileVO {

    private Long userId;
    private String username;
    private String nickname;
    private String avatarUrl;
    private String bio;
    private TeamBriefVO mainTeam;
    private Integer followingCount;
    private Integer followerCount;
    private Integer contentCount;
    private Integer likeReceivedCount;
    private UserRelationStatus relationStatus;
    private Boolean currentUser;

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
    public TeamBriefVO getMainTeam() { return mainTeam; }
    public void setMainTeam(TeamBriefVO mainTeam) { this.mainTeam = mainTeam; }
    public Integer getFollowingCount() { return followingCount; }
    public void setFollowingCount(Integer followingCount) { this.followingCount = followingCount; }
    public Integer getFollowerCount() { return followerCount; }
    public void setFollowerCount(Integer followerCount) { this.followerCount = followerCount; }
    public Integer getContentCount() { return contentCount; }
    public void setContentCount(Integer contentCount) { this.contentCount = contentCount; }
    public Integer getLikeReceivedCount() { return likeReceivedCount; }
    public void setLikeReceivedCount(Integer likeReceivedCount) { this.likeReceivedCount = likeReceivedCount; }
    public UserRelationStatus getRelationStatus() { return relationStatus; }
    public void setRelationStatus(UserRelationStatus relationStatus) { this.relationStatus = relationStatus; }
    public Boolean getCurrentUser() { return currentUser; }
    public void setCurrentUser(Boolean currentUser) { this.currentUser = currentUser; }
}
