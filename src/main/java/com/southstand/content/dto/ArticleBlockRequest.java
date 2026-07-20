package com.southstand.content.dto;

public class ArticleBlockRequest {

    private String blockType;
    private String text;
    private Long mediaFileId;
    private Integer sortOrder;

    public String getBlockType() { return blockType; }
    public void setBlockType(String blockType) { this.blockType = blockType; }
    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
    public Long getMediaFileId() { return mediaFileId; }
    public void setMediaFileId(Long mediaFileId) { this.mediaFileId = mediaFileId; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
}
