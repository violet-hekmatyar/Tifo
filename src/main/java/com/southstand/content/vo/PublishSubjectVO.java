package com.southstand.content.vo;

public class PublishSubjectVO {
    private Long subjectId;
    private String subjectType;
    private String name;
    private String summary;
    private String coverUrl;
    private long discussionCount;

    public Long getSubjectId() { return subjectId; }
    public void setSubjectId(Long subjectId) { this.subjectId = subjectId; }
    public String getSubjectType() { return subjectType; }
    public void setSubjectType(String subjectType) { this.subjectType = subjectType; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    public String getCoverUrl() { return coverUrl; }
    public void setCoverUrl(String coverUrl) { this.coverUrl = coverUrl; }
    public long getDiscussionCount() { return discussionCount; }
    public void setDiscussionCount(long discussionCount) { this.discussionCount = discussionCount; }
}
