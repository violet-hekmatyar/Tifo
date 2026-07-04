package com.southstand.user.vo;

public class UserFollowActionVO {

    private Long targetUserId;
    private Boolean followed;
    private UserRelationStatus relationStatus;
    private Integer followingCount;
    private Integer followerCount;

    public Long getTargetUserId() { return targetUserId; }
    public void setTargetUserId(Long targetUserId) { this.targetUserId = targetUserId; }
    public Boolean getFollowed() { return followed; }
    public void setFollowed(Boolean followed) { this.followed = followed; }
    public UserRelationStatus getRelationStatus() { return relationStatus; }
    public void setRelationStatus(UserRelationStatus relationStatus) { this.relationStatus = relationStatus; }
    public Integer getFollowingCount() { return followingCount; }
    public void setFollowingCount(Integer followingCount) { this.followingCount = followingCount; }
    public Integer getFollowerCount() { return followerCount; }
    public void setFollowerCount(Integer followerCount) { this.followerCount = followerCount; }
}
