package com.southstand.follow.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public class FollowToggleRequest {

    @NotBlank(message = "followType is required")
    @Pattern(regexp = "USER|TEAM|PLAYER", message = "followType must be USER, TEAM or PLAYER")
    private String followType;

    @NotNull(message = "targetId is required")
    private Long targetId;

    public String getFollowType() {
        return followType;
    }

    public void setFollowType(String followType) {
        this.followType = followType;
    }

    public Long getTargetId() {
        return targetId;
    }

    public void setTargetId(Long targetId) {
        this.targetId = targetId;
    }
}
