package com.southstand.admin.vo;

public class AdminDashboardSummaryVO {

    private Long userCount;
    private Long activeUserCount;
    private Long disabledUserCount;
    private Long contentCount;
    private Long publishedContentCount;
    private Long hiddenContentCount;
    private Long commentCount;
    private Long matchCount;
    private Long todayNewUserCount;
    private Long todayNewContentCount;

    public Long getUserCount() { return userCount; }
    public void setUserCount(Long userCount) { this.userCount = userCount; }
    public Long getActiveUserCount() { return activeUserCount; }
    public void setActiveUserCount(Long activeUserCount) { this.activeUserCount = activeUserCount; }
    public Long getDisabledUserCount() { return disabledUserCount; }
    public void setDisabledUserCount(Long disabledUserCount) { this.disabledUserCount = disabledUserCount; }
    public Long getContentCount() { return contentCount; }
    public void setContentCount(Long contentCount) { this.contentCount = contentCount; }
    public Long getPublishedContentCount() { return publishedContentCount; }
    public void setPublishedContentCount(Long publishedContentCount) { this.publishedContentCount = publishedContentCount; }
    public Long getHiddenContentCount() { return hiddenContentCount; }
    public void setHiddenContentCount(Long hiddenContentCount) { this.hiddenContentCount = hiddenContentCount; }
    public Long getCommentCount() { return commentCount; }
    public void setCommentCount(Long commentCount) { this.commentCount = commentCount; }
    public Long getMatchCount() { return matchCount; }
    public void setMatchCount(Long matchCount) { this.matchCount = matchCount; }
    public Long getTodayNewUserCount() { return todayNewUserCount; }
    public void setTodayNewUserCount(Long todayNewUserCount) { this.todayNewUserCount = todayNewUserCount; }
    public Long getTodayNewContentCount() { return todayNewContentCount; }
    public void setTodayNewContentCount(Long todayNewContentCount) { this.todayNewContentCount = todayNewContentCount; }
}
