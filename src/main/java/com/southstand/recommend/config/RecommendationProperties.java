package com.southstand.recommend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "recommendation")
public class RecommendationProperties {
    private boolean enabled = true;
    private final Experiment experiment = new Experiment();
    private final Cf cf = new Cf();

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public Experiment getExperiment() { return experiment; }
    public Cf getCf() { return cf; }

    public static class Experiment {
        private boolean enabled = true;
        private String id = "REC_HOME_V1";
        private int cfPercent = 50;
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean value) { enabled = value; }
        public String getId() { return id; }
        public void setId(String value) { id = value; }
        public int getCfPercent() { return cfPercent; }
        public void setCfPercent(int value) { cfPercent = Math.max(0, Math.min(100, value)); }
    }

    public static class Cf {
        private boolean enabled = true;
        private String baseUrl = "http://127.0.0.1:8100";
        private int connectTimeoutMs = 300;
        private int readTimeoutMs = 800;
        private double weight = 35D;
        private int failureThreshold = 3;
        private int cooldownSeconds = 30;
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean value) { enabled = value; }
        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String value) { baseUrl = value; }
        public int getConnectTimeoutMs() { return connectTimeoutMs; }
        public void setConnectTimeoutMs(int value) { connectTimeoutMs = value; }
        public int getReadTimeoutMs() { return readTimeoutMs; }
        public void setReadTimeoutMs(int value) { readTimeoutMs = value; }
        public double getWeight() { return weight; }
        public void setWeight(double value) { weight = value; }
        public int getFailureThreshold() { return failureThreshold; }
        public void setFailureThreshold(int value) { failureThreshold = Math.max(1, value); }
        public int getCooldownSeconds() { return cooldownSeconds; }
        public void setCooldownSeconds(int value) { cooldownSeconds = Math.max(1, value); }
    }
}

