package com.southstand.onboarding.dto;

import jakarta.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;

public class SavePreferencesRequest {

    @NotNull(message = "mainTeamId is required")
    private Long mainTeamId;

    private List<Long> followTeamIds = new ArrayList<>();
    private List<Long> followPlayerIds = new ArrayList<>();

    public Long getMainTeamId() {
        return mainTeamId;
    }

    public void setMainTeamId(Long mainTeamId) {
        this.mainTeamId = mainTeamId;
    }

    public List<Long> getFollowTeamIds() {
        return followTeamIds;
    }

    public void setFollowTeamIds(List<Long> followTeamIds) {
        this.followTeamIds = followTeamIds;
    }

    public List<Long> getFollowPlayerIds() {
        return followPlayerIds;
    }

    public void setFollowPlayerIds(List<Long> followPlayerIds) {
        this.followPlayerIds = followPlayerIds;
    }
}
