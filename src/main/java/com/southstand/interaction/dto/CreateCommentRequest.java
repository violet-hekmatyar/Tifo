package com.southstand.interaction.dto;

import jakarta.validation.constraints.Size;

public class CreateCommentRequest {

    private String targetType;

    private Long targetId;

    private Long contentId;

    private Long parentId = 0L;

    private Long replyToUserId;

    @Size(min = 1, max = 1000)
    private String contentText;

    private String content;

    public String getTargetType() { return targetType; }
    public void setTargetType(String targetType) { this.targetType = targetType; }
    public Long getTargetId() { return targetId; }
    public void setTargetId(Long targetId) { this.targetId = targetId; }
    public Long getContentId() { return contentId; }
    public void setContentId(Long contentId) { this.contentId = contentId; }
    public Long getParentId() { return parentId; }
    public void setParentId(Long parentId) { this.parentId = parentId; }
    public Long getReplyToUserId() { return replyToUserId; }
    public void setReplyToUserId(Long replyToUserId) { this.replyToUserId = replyToUserId; }
    public String getContentText() { return contentText; }
    public void setContentText(String contentText) { this.contentText = contentText; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
}
