package com.southstand.user.vo;

import java.util.ArrayList;
import java.util.List;

public class UserProfileVO {

    private Long userId;
    private String username;
    private String nickname;
    private String avatarUrl;
    private String bio;
    private String roleType;
    private String status;
    private Boolean onboardingCompleted;
    private TeamBriefVO mainTeam;
    private FollowStatsVO followStats;
    private List<TeamBriefVO> followTeams = new ArrayList<>();
    private List<PlayerBriefVO> followPlayers = new ArrayList<>();

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public String getRoleType() {
        return roleType;
    }

    public void setRoleType(String roleType) {
        this.roleType = roleType;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Boolean getOnboardingCompleted() {
        return onboardingCompleted;
    }

    public void setOnboardingCompleted(Boolean onboardingCompleted) {
        this.onboardingCompleted = onboardingCompleted;
    }

    public TeamBriefVO getMainTeam() {
        return mainTeam;
    }

    public void setMainTeam(TeamBriefVO mainTeam) {
        this.mainTeam = mainTeam;
    }

    public FollowStatsVO getFollowStats() {
        return followStats;
    }

    public void setFollowStats(FollowStatsVO followStats) {
        this.followStats = followStats;
    }

    public List<TeamBriefVO> getFollowTeams() {
        return followTeams;
    }

    public void setFollowTeams(List<TeamBriefVO> followTeams) {
        this.followTeams = followTeams;
    }

    public List<PlayerBriefVO> getFollowPlayers() {
        return followPlayers;
    }

    public void setFollowPlayers(List<PlayerBriefVO> followPlayers) {
        this.followPlayers = followPlayers;
    }
}
