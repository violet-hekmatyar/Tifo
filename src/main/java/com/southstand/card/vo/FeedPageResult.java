package com.southstand.card.vo;

import java.util.List;

public class FeedPageResult {

    private List<FeedCardVO> records;
    private long total;
    private long pageNum;
    private long pageSize;
    private long pages;
    private String nextCursor;

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
}
