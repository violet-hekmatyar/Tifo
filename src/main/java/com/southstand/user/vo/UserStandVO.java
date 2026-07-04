package com.southstand.user.vo;

import java.util.List;

public class UserStandVO {

    private Long userId;
    private String username;
    private String nickname;
    private String avatarUrl;
    private String bio;
    private TeamBriefVO mainTeam;
    private List<TeamBriefVO> followTeams;
    private List<PlayerBriefVO> followPlayers;
    private Integer followingUserCount;
    private Integer followerCount;
    private Integer contentCount;
    private Integer likeReceivedCount;

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
    public List<TeamBriefVO> getFollowTeams() { return followTeams; }
    public void setFollowTeams(List<TeamBriefVO> followTeams) { this.followTeams = followTeams; }
    public List<PlayerBriefVO> getFollowPlayers() { return followPlayers; }
    public void setFollowPlayers(List<PlayerBriefVO> followPlayers) { this.followPlayers = followPlayers; }
    public Integer getFollowingUserCount() { return followingUserCount; }
    public void setFollowingUserCount(Integer followingUserCount) { this.followingUserCount = followingUserCount; }
    public Integer getFollowerCount() { return followerCount; }
    public void setFollowerCount(Integer followerCount) { this.followerCount = followerCount; }
    public Integer getContentCount() { return contentCount; }
    public void setContentCount(Integer contentCount) { this.contentCount = contentCount; }
    public Integer getLikeReceivedCount() { return likeReceivedCount; }
    public void setLikeReceivedCount(Integer likeReceivedCount) { this.likeReceivedCount = likeReceivedCount; }
}
