package com.southstand.user.vo;

public class FollowStatsVO {

    private Integer followingCount;
    private Integer followerCount;
    private Integer teamFollowCount;
    private Integer playerFollowCount;

    public Integer getFollowingCount() {
        return followingCount;
    }

    public void setFollowingCount(Integer followingCount) {
        this.followingCount = followingCount;
    }

    public Integer getFollowerCount() {
        return followerCount;
    }

    public void setFollowerCount(Integer followerCount) {
        this.followerCount = followerCount;
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
}
