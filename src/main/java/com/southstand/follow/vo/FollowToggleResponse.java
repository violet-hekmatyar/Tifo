package com.southstand.follow.vo;

public class FollowToggleResponse {

    private String followType;
    private Long targetId;
    private Boolean followed;
    private Integer teamFollowCount;
    private Integer playerFollowCount;
    private Integer followingCount;

    public String getFollowType() {
        return followType;
    }

    public void setFollowType(String followType) {
        this.followType = followType;
    }

    public Long getTargetId() {
        return targetId;
    }

    public void setTargetId(Long targetId) {
        this.targetId = targetId;
    }

    public Boolean getFollowed() {
        return followed;
    }

    public void setFollowed(Boolean followed) {
        this.followed = followed;
    }

    public Integer getTeamFollowCount() {
        return teamFollowCount;
    }

    public void setTeamFollowCount(Integer teamFollowCount) {
        this.teamFollowCount = teamFollowCount;
    }

    public Integer getPlayerFollowCount() {
        return playerFollowCount;
    }

    public void setPlayerFollowCount(Integer playerFollowCount) {
        this.playerFollowCount = playerFollowCount;
    }

    public Integer getFollowingCount() {
        return followingCount;
    }

    public void setFollowingCount(Integer followingCount) {
        this.followingCount = followingCount;
    }
}
