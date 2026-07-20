package com.southstand.content.vo;

public class ArticleBlockVO {

    private Long blockId;
    private String blockType;
    private String text;
    private Long mediaFileId;
    private String mediaUrl;
    private Integer sortOrder;

    public Long getBlockId() { return blockId; }
    public void setBlockId(Long blockId) { this.blockId = blockId; }
    public String getBlockType() { return blockType; }
    public void setBlockType(String blockType) { this.blockType = blockType; }
    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
    public Long getMediaFileId() { return mediaFileId; }
    public void setMediaFileId(Long mediaFileId) { this.mediaFileId = mediaFileId; }
    public String getMediaUrl() { return mediaUrl; }
    public void setMediaUrl(String mediaUrl) { this.mediaUrl = mediaUrl; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
}
