package com.southstand.recommend.vo;

import com.southstand.recommend.model.RecommendationItem;
import java.util.ArrayList;
import java.util.List;

public class RecommendationResult {
    private String algorithmVersion;
    private String modelVersion;
    private String experimentId;
    private String experimentBucket;
    private String requestId;
    private boolean cfFallback;
    private String fallbackReason;
    private List<RecommendationItem> items = new ArrayList<>();

    public String getAlgorithmVersion() { return algorithmVersion; }
    public void setAlgorithmVersion(String value) { algorithmVersion = value; }
    public String getModelVersion() { return modelVersion; }
    public void setModelVersion(String value) { modelVersion = value; }
    public String getExperimentId() { return experimentId; }
    public void setExperimentId(String value) { experimentId = value; }
    public String getExperimentBucket() { return experimentBucket; }
    public void setExperimentBucket(String value) { experimentBucket = value; }
    public String getRequestId() { return requestId; }
    public void setRequestId(String value) { requestId = value; }
    public boolean isCfFallback() { return cfFallback; }
    public void setCfFallback(boolean value) { cfFallback = value; }
    public String getFallbackReason() { return fallbackReason; }
    public void setFallbackReason(String value) { fallbackReason = value; }
    public List<RecommendationItem> getItems() { return items; }
    public void setItems(List<RecommendationItem> value) { items = value == null ? new ArrayList<>() : value; }
}

