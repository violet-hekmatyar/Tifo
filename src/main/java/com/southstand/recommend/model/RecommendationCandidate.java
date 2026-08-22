package com.southstand.recommend.model;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

public class RecommendationCandidate {
    private String cardType;
    private RecommendationTargetType targetType;
    private Long targetId;
    private String contentType;
    private Long authorId;
    private Set<Long> teamIds = new LinkedHashSet<>();
    private Set<Long> playerIds = new LinkedHashSet<>();
    private Long relatedMatchId;
    private int likeCount;
    private int commentCount;
    private int favoriteCount;
    private double hotScore;
    private LocalDateTime publishTime;
    private String matchStatus;
    private Integer importantLevel;
    private boolean hasReport;
    private Long homeTeamId;
    private Long awayTeamId;
    private LocalDateTime matchTime;

    public String key() { return targetType + "_" + targetId; }
    public String getCardType() { return cardType; }
    public void setCardType(String cardType) { this.cardType = cardType; }
    public RecommendationTargetType getTargetType() { return targetType; }
    public void setTargetType(RecommendationTargetType targetType) { this.targetType = targetType; }
    public Long getTargetId() { return targetId; }
    public void setTargetId(Long targetId) { this.targetId = targetId; }
    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }
    public Long getAuthorId() { return authorId; }
    public void setAuthorId(Long authorId) { this.authorId = authorId; }
    public Set<Long> getTeamIds() { return teamIds; }
    public void setTeamIds(Set<Long> teamIds) { this.teamIds = teamIds == null ? new LinkedHashSet<>() : teamIds; }
    public Set<Long> getPlayerIds() { return playerIds; }
    public void setPlayerIds(Set<Long> playerIds) { this.playerIds = playerIds == null ? new LinkedHashSet<>() : playerIds; }
    public Long getRelatedMatchId() { return relatedMatchId; }
    public void setRelatedMatchId(Long relatedMatchId) { this.relatedMatchId = relatedMatchId; }
    public int getLikeCount() { return likeCount; }
    public void setLikeCount(int likeCount) { this.likeCount = likeCount; }
    public int getCommentCount() { return commentCount; }
    public void setCommentCount(int commentCount) { this.commentCount = commentCount; }
    public int getFavoriteCount() { return favoriteCount; }
    public void setFavoriteCount(int favoriteCount) { this.favoriteCount = favoriteCount; }
    public double getHotScore() { return hotScore; }
    public void setHotScore(double hotScore) { this.hotScore = hotScore; }
    public LocalDateTime getPublishTime() { return publishTime; }
    public void setPublishTime(LocalDateTime publishTime) { this.publishTime = publishTime; }
    public String getMatchStatus() { return matchStatus; }
    public void setMatchStatus(String matchStatus) { this.matchStatus = matchStatus; }
    public Integer getImportantLevel() { return importantLevel; }
    public void setImportantLevel(Integer importantLevel) { this.importantLevel = importantLevel; }
    public boolean isHasReport() { return hasReport; }
    public void setHasReport(boolean hasReport) { this.hasReport = hasReport; }
    public Long getHomeTeamId() { return homeTeamId; }
    public void setHomeTeamId(Long homeTeamId) { this.homeTeamId = homeTeamId; }
    public Long getAwayTeamId() { return awayTeamId; }
    public void setAwayTeamId(Long awayTeamId) { this.awayTeamId = awayTeamId; }
    public LocalDateTime getMatchTime() { return matchTime; }
    public void setMatchTime(LocalDateTime matchTime) { this.matchTime = matchTime; }
}

