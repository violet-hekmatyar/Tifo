package com.southstand.content.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("content_relation")
public class ContentRelation {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long contentId;
    private String relationType;
    private Long relationId;
    private BigDecimal confidence;
    private String sourceType;
    private String status;
    private Integer isDeleted;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getContentId() { return contentId; }
    public void setContentId(Long contentId) { this.contentId = contentId; }
    public String getRelationType() { return relationType; }
    public void setRelationType(String relationType) { this.relationType = relationType; }
    public Long getRelationId() { return relationId; }
    public void setRelationId(Long relationId) { this.relationId = relationId; }
    public BigDecimal getConfidence() { return confidence; }
    public void setConfidence(BigDecimal confidence) { this.confidence = confidence; }
    public String getSourceType() { return sourceType; }
    public void setSourceType(String sourceType) { this.sourceType = sourceType; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Integer getIsDeleted() { return isDeleted; }
    public void setIsDeleted(Integer isDeleted) { this.isDeleted = isDeleted; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
}
