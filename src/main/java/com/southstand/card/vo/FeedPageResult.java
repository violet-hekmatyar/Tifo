package com.southstand.card.vo;

import java.util.List;

public class FeedPageResult {

    private List<FeedCardVO> records;
    private long total;
    private long pageNum;
    private long pageSize;
    private long pages;
    private String nextCursor;
    private String algorithmVersion;
    private String modelVersion;
    private String experimentId;
    private String experimentBucket;
    private String requestId;

    public static FeedPageResult of(List<FeedCardVO> records, long total, long pageNum, long pageSize) {
        FeedPageResult result = new FeedPageResult();
        result.setRecords(records);
        result.setTotal(total);
        result.setPageNum(pageNum);
        result.setPageSize(pageSize);
        result.setPages(pageSize <= 0 ? 0 : (total + pageSize - 1) / pageSize);
        result.setNextCursor(null);
        return result;
    }

    public List<FeedCardVO> getRecords() { return records; }
    public void setRecords(List<FeedCardVO> records) { this.records = records; }
    public long getTotal() { return total; }
    public void setTotal(long total) { this.total = total; }
    public long getPageNum() { return pageNum; }
    public void setPageNum(long pageNum) { this.pageNum = pageNum; }
    public long getPageSize() { return pageSize; }
    public void setPageSize(long pageSize) { this.pageSize = pageSize; }
    public long getPages() { return pages; }
    public void setPages(long pages) { this.pages = pages; }
    public String getNextCursor() { return nextCursor; }
    public void setNextCursor(String nextCursor) { this.nextCursor = nextCursor; }
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
}
