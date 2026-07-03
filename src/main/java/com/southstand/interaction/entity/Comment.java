package com.southstand.interaction.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("comment")
public class Comment {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private String targetType;
    private Long targetId;
    private Long parentId;
    private Long userId;
    private String contentText;
    private Integer likeCount;
    private Integer replyCount;
    private BigDecimal hotScore;
    private Integer isTop;
    private BigDecimal userRatingScore;
    private String status;
    private Integer isDeleted;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTargetType() { return targetType; }
    public void setTargetType(String targetType) { this.targetType = targetType; }
    public Long getTargetId() { return targetId; }
    public void setTargetId(Long targetId) { this.targetId = targetId; }
    public Long getParentId() { return parentId; }
    public void setParentId(Long parentId) { this.parentId = parentId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getContentText() { return contentText; }
    public void setContentText(String contentText) { this.contentText = contentText; }
    public Integer getLikeCount() { return likeCount; }
    public void setLikeCount(Integer likeCount) { this.likeCount = likeCount; }
    public Integer getReplyCount() { return replyCount; }
    public void setReplyCount(Integer replyCount) { this.replyCount = replyCount; }
    public BigDecimal getHotScore() { return hotScore; }
    public void setHotScore(BigDecimal hotScore) { this.hotScore = hotScore; }
    public Integer getIsTop() { return isTop; }
    public void setIsTop(Integer isTop) { this.isTop = isTop; }
    public BigDecimal getUserRatingScore() { return userRatingScore; }
    public void setUserRatingScore(BigDecimal userRatingScore) { this.userRatingScore = userRatingScore; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Integer getIsDeleted() { return isDeleted; }
    public void setIsDeleted(Integer isDeleted) { this.isDeleted = isDeleted; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
}
