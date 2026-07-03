package com.southstand.interaction.vo;

import java.time.LocalDateTime;

public class CreateCommentResponse {
    private Long commentId;
    private Long parentId;
    private LocalDateTime createTime;

    public Long getCommentId() { return commentId; }
    public void setCommentId(Long commentId) { this.commentId = commentId; }
    public Long getParentId() { return parentId; }
    public void setParentId(Long parentId) { this.parentId = parentId; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}
