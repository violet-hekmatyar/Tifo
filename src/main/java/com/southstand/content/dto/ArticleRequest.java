package com.southstand.content.dto;

import java.util.List;

public class ArticleRequest {

    private String title;
    private String summary;
    private Long coverFileId;
    private List<ArticleBlockRequest> blocks;
    private List<ContentRelationRequest> relationList;

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    public Long getCoverFileId() { return coverFileId; }
    public void setCoverFileId(Long coverFileId) { this.coverFileId = coverFileId; }
    public List<ArticleBlockRequest> getBlocks() { return blocks; }
    public void setBlocks(List<ArticleBlockRequest> blocks) { this.blocks = blocks; }
    public List<ContentRelationRequest> getRelationList() { return relationList; }
    public void setRelationList(List<ContentRelationRequest> relationList) { this.relationList = relationList; }
}
