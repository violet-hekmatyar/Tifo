package com.southstand.recommend.vo;

public class RecommendationBehaviorBatchResult {
    private int received;
    private int saved;
    private int duplicated;
    private int rejected;

    public int getReceived() { return received; }
    public void setReceived(int received) { this.received = received; }
    public int getSaved() { return saved; }
    public void setSaved(int saved) { this.saved = saved; }
    public int getDuplicated() { return duplicated; }
    public void setDuplicated(int duplicated) { this.duplicated = duplicated; }
    public int getRejected() { return rejected; }
    public void setRejected(int rejected) { this.rejected = rejected; }
}
