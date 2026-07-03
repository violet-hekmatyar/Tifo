package com.southstand.user.vo;

import java.util.List;

public class UserSummaryVO {

    private Long userId;
    private String username;
    private String nickname;
    private String avatarUrl;
    private String bio;
    private String roleType;
    private String status;
    private Boolean onboardingCompleted;
    private TeamBriefVO mainTeam;
    private MyStatsVO stats;
    private List<MyContentVO> recentContents;
    private List<MyFavoriteVO> recentFavorites;
    private List<MyCommentVO> recentComments;

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
    public String getRoleType() { return roleType; }
    public void setRoleType(String roleType) { this.roleType = roleType; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Boolean getOnboardingCompleted() { return onboardingCompleted; }
    public void setOnboardingCompleted(Boolean onboardingCompleted) { this.onboardingCompleted = onboardingCompleted; }
    public TeamBriefVO getMainTeam() { return mainTeam; }
    public void setMainTeam(TeamBriefVO mainTeam) { this.mainTeam = mainTeam; }
    public MyStatsVO getStats() { return stats; }
    public void setStats(MyStatsVO stats) { this.stats = stats; }
    public List<MyContentVO> getRecentContents() { return recentContents; }
    public void setRecentContents(List<MyContentVO> recentContents) { this.recentContents = recentContents; }
    public List<MyFavoriteVO> getRecentFavorites() { return recentFavorites; }
    public void setRecentFavorites(List<MyFavoriteVO> recentFavorites) { this.recentFavorites = recentFavorites; }
    public List<MyCommentVO> getRecentComments() { return recentComments; }
    public void setRecentComments(List<MyCommentVO> recentComments) { this.recentComments = recentComments; }
}
