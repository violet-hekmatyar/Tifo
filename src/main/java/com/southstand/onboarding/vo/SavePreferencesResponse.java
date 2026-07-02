package com.southstand.onboarding.vo;

public class SavePreferencesResponse {

    private Boolean completed;
    private Long mainTeamId;
    private Integer followTeamCount;
    private Integer followPlayerCount;

    public Boolean getCompleted() {
        return completed;
    }

    public void setCompleted(Boolean completed) {
        this.completed = completed;
    }

    public Long getMainTeamId() {
        return mainTeamId;
    }

    public void setMainTeamId(Long mainTeamId) {
        this.mainTeamId = mainTeamId;
    }

    public Integer getFollowTeamCount() {
        return followTeamCount;
    }

    public void setFollowTeamCount(Integer followTeamCount) {
        this.followTeamCount = followTeamCount;
    }

    public Integer getFollowPlayerCount() {
        return followPlayerCount;
    }

    public void setFollowPlayerCount(Integer followPlayerCount) {
        this.followPlayerCount = followPlayerCount;
    }
}
