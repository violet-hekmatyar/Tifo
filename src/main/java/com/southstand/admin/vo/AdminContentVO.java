package com.southstand.admin.vo;

import java.time.LocalDateTime;

public class AdminContentVO {

    private Long contentId;
    private String contentType;
    private String title;
    private String summary;
    private Long authorId;
    private String authorNickname;
    private String status;
    private Integer likeCount;
    private Integer commentCount;
    private Integer favoriteCount;
    private LocalDateTime publishTime;
    private LocalDateTime createTime;

    public Long getContentId() { return contentId; }
    public void setContentId(Long contentId) { this.contentId = contentId; }
    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    public Long getAuthorId() { return authorId; }
    public void setAuthorId(Long authorId) { this.authorId = authorId; }
    public String getAuthorNickname() { return authorNickname; }
    public void setAuthorNickname(String authorNickname) { this.authorNickname = authorNickname; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Integer getLikeCount() { return likeCount; }
    public void setLikeCount(Integer likeCount) { this.likeCount = likeCount; }
    public Integer getCommentCount() { return commentCount; }
    public void setCommentCount(Integer commentCount) { this.commentCount = commentCount; }
    public Integer getFavoriteCount() { return favoriteCount; }
    public void setFavoriteCount(Integer favoriteCount) { this.favoriteCount = favoriteCount; }
    public LocalDateTime getPublishTime() { return publishTime; }
    public void setPublishTime(LocalDateTime publishTime) { this.publishTime = publishTime; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}
