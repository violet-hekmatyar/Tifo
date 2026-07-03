package com.southstand.content.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("content")
public class Content {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private String contentType;
    private String contentFormat;
    private String cardType;
    private String title;
    private String summary;
    private String body;
    private String coverUrl;
    private Long authorId;
    private String sourceType;
    private String sourceName;
    private Integer isOfficial;
    private Integer viewCount;
    private Integer likeCount;
    private Integer commentCount;
    private Integer favoriteCount;
    private BigDecimal hotScore;
    private LocalDateTime publishTime;
    private String status;
    private Integer isDeleted;
    private String extraJson;
    private String remark;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
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
    public Long getAuthorId() { return authorId; }
    public void setAuthorId(Long authorId) { this.authorId = authorId; }
    public String getSourceType() { return sourceType; }
    public void setSourceType(String sourceType) { this.sourceType = sourceType; }
    public String getSourceName() { return sourceName; }
    public void setSourceName(String sourceName) { this.sourceName = sourceName; }
    public Integer getIsOfficial() { return isOfficial; }
    public void setIsOfficial(Integer isOfficial) { this.isOfficial = isOfficial; }
    public Integer getViewCount() { return viewCount; }
    public void setViewCount(Integer viewCount) { this.viewCount = viewCount; }
    public Integer getLikeCount() { return likeCount; }
    public void setLikeCount(Integer likeCount) { this.likeCount = likeCount; }
    public Integer getCommentCount() { return commentCount; }
    public void setCommentCount(Integer commentCount) { this.commentCount = commentCount; }
    public Integer getFavoriteCount() { return favoriteCount; }
    public void setFavoriteCount(Integer favoriteCount) { this.favoriteCount = favoriteCount; }
    public BigDecimal getHotScore() { return hotScore; }
    public void setHotScore(BigDecimal hotScore) { this.hotScore = hotScore; }
    public LocalDateTime getPublishTime() { return publishTime; }
    public void setPublishTime(LocalDateTime publishTime) { this.publishTime = publishTime; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Integer getIsDeleted() { return isDeleted; }
    public void setIsDeleted(Integer isDeleted) { this.isDeleted = isDeleted; }
    public String getExtraJson() { return extraJson; }
    public void setExtraJson(String extraJson) { this.extraJson = extraJson; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
}
