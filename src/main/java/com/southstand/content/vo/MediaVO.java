package com.southstand.content.vo;

public class MediaVO {
    private Long mediaId;
    private String mediaType;
    private String mediaUrl;
    private String thumbnailUrl;
    private Integer width;
    private Integer height;

    public Long getMediaId() { return mediaId; }
    public void setMediaId(Long mediaId) { this.mediaId = mediaId; }
    public String getMediaType() { return mediaType; }
    public void setMediaType(String mediaType) { this.mediaType = mediaType; }
    public String getMediaUrl() { return mediaUrl; }
    public void setMediaUrl(String mediaUrl) { this.mediaUrl = mediaUrl; }
    public String getThumbnailUrl() { return thumbnailUrl; }
    public void setThumbnailUrl(String thumbnailUrl) { this.thumbnailUrl = thumbnailUrl; }
    public Integer getWidth() { return width; }
    public void setWidth(Integer width) { this.width = width; }
    public Integer getHeight() { return height; }
    public void setHeight(Integer height) { this.height = height; }
}
