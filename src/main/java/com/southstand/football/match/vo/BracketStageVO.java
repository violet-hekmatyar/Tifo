package com.southstand.football.match.vo;

import java.util.List;

public class BracketStageVO {

    private Long stageId;
    private String stageType;
    private String stageName;
    private Integer sortOrder;
    private List<BracketTieVO> ties;

    public Long getStageId() { return stageId; }
    public void setStageId(Long stageId) { this.stageId = stageId; }
    public String getStageType() { return stageType; }
    public void setStageType(String stageType) { this.stageType = stageType; }
    public String getStageName() { return stageName; }
    public void setStageName(String stageName) { this.stageName = stageName; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
    public List<BracketTieVO> getTies() { return ties; }
    public void setTies(List<BracketTieVO> ties) { this.ties = ties; }
}
