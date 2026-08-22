package com.southstand.recommend.model;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class RecommendationContext {
    private Long userId;
    private Long mainTeamId;
    private Set<Long> followedTeamIds = new HashSet<>();
    private Set<Long> followedPlayerIds = new HashSet<>();
    private Set<Long> followedUserIds = new HashSet<>();
    private Set<String> exposedKeys = new HashSet<>();
    private String scene;
    private List<RecommendationCandidate> candidates = new ArrayList<>();

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getMainTeamId() { return mainTeamId; }
    public void setMainTeamId(Long mainTeamId) { this.mainTeamId = mainTeamId; }
    public Set<Long> getFollowedTeamIds() { return followedTeamIds; }
    public void setFollowedTeamIds(Set<Long> value) { followedTeamIds = value == null ? new HashSet<>() : value; }
    public Set<Long> getFollowedPlayerIds() { return followedPlayerIds; }
    public void setFollowedPlayerIds(Set<Long> value) { followedPlayerIds = value == null ? new HashSet<>() : value; }
    public Set<Long> getFollowedUserIds() { return followedUserIds; }
    public void setFollowedUserIds(Set<Long> value) { followedUserIds = value == null ? new HashSet<>() : value; }
    public Set<String> getExposedKeys() { return exposedKeys; }
    public void setExposedKeys(Set<String> value) { exposedKeys = value == null ? new HashSet<>() : value; }
    public String getScene() { return scene; }
    public void setScene(String scene) { this.scene = scene; }
    public List<RecommendationCandidate> getCandidates() { return candidates; }
    public void setCandidates(List<RecommendationCandidate> value) { candidates = value == null ? new ArrayList<>() : value; }
}

