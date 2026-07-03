package com.southstand.user.vo;

import java.time.LocalDateTime;

public class MyFavoriteVO {

    private Long favoriteId;
    private String targetType;
    private Long targetId;
    private String contentType;
    private String title;
    private String summary;
    private String coverUrl;
    private String contentStatus;
    private LocalDateTime favoriteTime;
    private LocalDateTime publishTime;

    public Long getFavoriteId() { return favoriteId; }
    public void setFavoriteId(Long favoriteId) { this.favoriteId = favoriteId; }
    public String getTargetType() { return targetType; }
    public void setTargetType(String targetType) { this.targetType = targetType; }
    public Long getTargetId() { return targetId; }
    public void setTargetId(Long targetId) { this.targetId = targetId; }
    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    public String getCoverUrl() { return coverUrl; }
    public void setCoverUrl(String coverUrl) { this.coverUrl = coverUrl; }
    public String getContentStatus() { return contentStatus; }
    public void setContentStatus(String contentStatus) { this.contentStatus = contentStatus; }
    public LocalDateTime getFavoriteTime() { return favoriteTime; }
    public void setFavoriteTime(LocalDateTime favoriteTime) { this.favoriteTime = favoriteTime; }
    public LocalDateTime getPublishTime() { return publishTime; }
    public void setPublishTime(LocalDateTime publishTime) { this.publishTime = publishTime; }
}
