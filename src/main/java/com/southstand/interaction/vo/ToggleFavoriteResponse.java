package com.southstand.interaction.vo;

public class ToggleFavoriteResponse {
    private String targetType;
    private Long targetId;
    private Boolean favorited;

    public String getTargetType() { return targetType; }
    public void setTargetType(String targetType) { this.targetType = targetType; }
    public Long getTargetId() { return targetId; }
    public void setTargetId(Long targetId) { this.targetId = targetId; }
    public Boolean getFavorited() { return favorited; }
    public void setFavorited(Boolean favorited) { this.favorited = favorited; }
}
