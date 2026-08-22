package com.southstand.recommend.dto;

import java.util.ArrayList;
import java.util.List;

public class CfContentScoreRequest {
    private Long userId;
    private List<Long> candidateIds = new ArrayList<>();
    public CfContentScoreRequest() { }
    public CfContentScoreRequest(Long userId, List<Long> candidateIds) {
        this.userId = userId;
        this.candidateIds = candidateIds;
    }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public List<Long> getCandidateIds() { return candidateIds; }
    public void setCandidateIds(List<Long> candidateIds) { this.candidateIds = candidateIds; }
}

