package com.southstand.content.vo;

import java.time.LocalDateTime;
import java.util.List;

public class ContentDetailVO {
    private Long contentId;
    private String contentType;
    private String contentFormat;
    private String cardType;
    private String title;
    private String summary;
    private String body;
    private String coverUrl;
    private AuthorVO author;
    private List<ArticleBlockVO> blocks;
    private List<MediaVO> mediaList;
    private List<RelationVO> relationList;
    private Integer likeCount;
    private Integer commentCount;
    private Integer favoriteCount;
    private Integer viewCount;
    private Boolean liked;
    private Boolean favorited;
    private LocalDateTime publishTime;

    public Long getContentId() { return contentId; }
    public void setContentId(Long contentId) { this.contentId = contentId; }
    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }
    public String getContentFormat() { return contentFormat; }
    public void setContentFormat(String contentFormat) { this.contentFormat = contentFormat; }
    public String getCardType() { return cardType; }
    public void setCardType(String cardType) { this.cardType = cardType; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }
    public String getCoverUrl() { return coverUrl; }
    public void setCoverUrl(String coverUrl) { this.coverUrl = coverUrl; }
    public AuthorVO getAuthor() { return author; }
    public void setAuthor(AuthorVO author) { this.author = author; }
    public List<ArticleBlockVO> getBlocks() { return blocks; }
    public void setBlocks(List<ArticleBlockVO> blocks) { this.blocks = blocks; }
    public List<MediaVO> getMediaList() { return mediaList; }
    public void setMediaList(List<MediaVO> mediaList) { this.mediaList = mediaList; }
    public List<RelationVO> getRelationList() { return relationList; }
    public void setRelationList(List<RelationVO> relationList) { this.relationList = relationList; }
    public Integer getLikeCount() { return likeCount; }
    public void setLikeCount(Integer likeCount) { this.likeCount = likeCount; }
    public Integer getCommentCount() { return commentCount; }
    public void setCommentCount(Integer commentCount) { this.commentCount = commentCount; }
    public Integer getFavoriteCount() { return favoriteCount; }
    public void setFavoriteCount(Integer favoriteCount) { this.favoriteCount = favoriteCount; }
    public Integer getViewCount() { return viewCount; }
    public void setViewCount(Integer viewCount) { this.viewCount = viewCount; }
    public Boolean getLiked() { return liked; }
    public void setLiked(Boolean liked) { this.liked = liked; }
    public Boolean getFavorited() { return favorited; }
    public void setFavorited(Boolean favorited) { this.favorited = favorited; }
    public LocalDateTime getPublishTime() { return publishTime; }
    public void setPublishTime(LocalDateTime publishTime) { this.publishTime = publishTime; }
}
