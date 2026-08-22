package com.southstand.card.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "feed.auxiliary-cards")
public class HomeCardProperties {
    private boolean enabled = true;
    private double maxRatio = 0.20D;
    private int minGap = 3;

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean value) { enabled = value; }
    public double getMaxRatio() { return maxRatio; }
    public void setMaxRatio(double value) { maxRatio = Math.max(0D, Math.min(0.5D, value)); }
    public int getMinGap() { return minGap; }
    public void setMinGap(int value) { minGap = Math.max(1, value); }
}
