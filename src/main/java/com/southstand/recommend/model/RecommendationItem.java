package com.southstand.recommend.model;

public class RecommendationItem {
    private RecommendationTargetType targetType;
    private Long targetId;
    private double score;
    private double ruleScore;
    private double cfScore;
    private RecommendationReasonCode reasonCode;

    public String key() { return targetType + "_" + targetId; }
    public RecommendationTargetType getTargetType() { return targetType; }
    public void setTargetType(RecommendationTargetType targetType) { this.targetType = targetType; }
    public Long getTargetId() { return targetId; }
    public void setTargetId(Long targetId) { this.targetId = targetId; }
    public double getScore() { return score; }
    public void setScore(double score) { this.score = score; }
    public double getRuleScore() { return ruleScore; }
    public void setRuleScore(double ruleScore) { this.ruleScore = ruleScore; }
    public double getCfScore() { return cfScore; }
    public void setCfScore(double cfScore) { this.cfScore = cfScore; }
    public RecommendationReasonCode getReasonCode() { return reasonCode; }
    public void setReasonCode(RecommendationReasonCode reasonCode) { this.reasonCode = reasonCode; }
}

