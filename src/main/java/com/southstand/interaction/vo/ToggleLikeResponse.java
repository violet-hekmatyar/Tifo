package com.southstand.interaction.vo;

public class ToggleLikeResponse {
    private String targetType;
    private Long targetId;
    private Boolean liked;

    public String getTargetType() { return targetType; }
    public void setTargetType(String targetType) { this.targetType = targetType; }
    public Long getTargetId() { return targetId; }
    public void setTargetId(Long targetId) { this.targetId = targetId; }
    public Boolean getLiked() { return liked; }
    public void setLiked(Boolean liked) { this.liked = liked; }
}
