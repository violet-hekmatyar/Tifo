package com.southstand.recommend.dto;

import java.util.ArrayList;
import java.util.List;

public class CfContentScoreResponse {
    private boolean success;
    private boolean modelReady;
    private String algorithmVersion;
    private String modelVersion;
    private List<Item> items = new ArrayList<>();

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    public boolean isModelReady() { return modelReady; }
    public void setModelReady(boolean modelReady) { this.modelReady = modelReady; }
    public String getAlgorithmVersion() { return algorithmVersion; }
    public void setAlgorithmVersion(String algorithmVersion) { this.algorithmVersion = algorithmVersion; }
    public String getModelVersion() { return modelVersion; }
    public void setModelVersion(String modelVersion) { this.modelVersion = modelVersion; }
    public List<Item> getItems() { return items; }
    public void setItems(List<Item> items) { this.items = items; }

    public static class Item {
        private Long contentId;
        private Double cfScore;
        public Long getContentId() { return contentId; }
        public void setContentId(Long contentId) { this.contentId = contentId; }
        public Double getCfScore() { return cfScore; }
        public void setCfScore(Double cfScore) { this.cfScore = cfScore; }
    }
}

