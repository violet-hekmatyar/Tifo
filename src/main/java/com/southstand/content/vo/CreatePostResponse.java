package com.southstand.content.vo;

import java.time.LocalDateTime;

public class CreatePostResponse {
    private Long contentId;
    private String contentType;
    private String title;
    private LocalDateTime publishTime;

    public Long getContentId() { return contentId; }
    public void setContentId(Long contentId) { this.contentId = contentId; }
    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public LocalDateTime getPublishTime() { return publishTime; }
    public void setPublishTime(LocalDateTime publishTime) { this.publishTime = publishTime; }
}
