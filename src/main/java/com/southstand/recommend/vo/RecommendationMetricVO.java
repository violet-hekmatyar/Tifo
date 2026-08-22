package com.southstand.recommend.vo;

public class RecommendationMetricVO {
    private long exposureCount;
    private long clickCount;
    private double ctr;
    private long detailCount;
    private double avgDwellMs;
    private long likeCount;
    private long favoriteCount;
    private long commentCount;
    private double interactionRate;
    private long cfRequestCount;
    private long cfSuccessCount;
    private long cfFallbackCount;
    private double cfSuccessRate;
    private double fallbackRate;
    private double avgCfLatencyMs;

    public long getExposureCount() { return exposureCount; }
    public void setExposureCount(long value) { exposureCount = value; }
    public long getClickCount() { return clickCount; }
    public void setClickCount(long value) { clickCount = value; }
    public double getCtr() { return ctr; }
    public void setCtr(double value) { ctr = value; }
    public long getDetailCount() { return detailCount; }
    public void setDetailCount(long value) { detailCount = value; }
    public double getAvgDwellMs() { return avgDwellMs; }
    public void setAvgDwellMs(double value) { avgDwellMs = value; }
    public long getLikeCount() { return likeCount; }
    public void setLikeCount(long value) { likeCount = value; }
    public long getFavoriteCount() { return favoriteCount; }
    public void setFavoriteCount(long value) { favoriteCount = value; }
    public long getCommentCount() { return commentCount; }
    public void setCommentCount(long value) { commentCount = value; }
    public double getInteractionRate() { return interactionRate; }
    public void setInteractionRate(double value) { interactionRate = value; }
    public long getCfRequestCount() { return cfRequestCount; }
    public void setCfRequestCount(long value) { cfRequestCount = value; }
    public long getCfSuccessCount() { return cfSuccessCount; }
    public void setCfSuccessCount(long value) { cfSuccessCount = value; }
    public long getCfFallbackCount() { return cfFallbackCount; }
    public void setCfFallbackCount(long value) { cfFallbackCount = value; }
    public double getCfSuccessRate() { return cfSuccessRate; }
    public void setCfSuccessRate(double value) { cfSuccessRate = value; }
    public double getFallbackRate() { return fallbackRate; }
    public void setFallbackRate(double value) { fallbackRate = value; }
    public double getAvgCfLatencyMs() { return avgCfLatencyMs; }
    public void setAvgCfLatencyMs(double value) { avgCfLatencyMs = value; }
}

