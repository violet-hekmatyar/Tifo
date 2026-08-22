package com.southstand.card.vo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.southstand.content.vo.AuthorVO;
import com.southstand.football.match.vo.MatchTeamVO;
import com.southstand.interaction.vo.HotCommentVO;
import java.time.LocalDateTime;
import java.util.List;

public class FeedCardVO {

    private String cardId;
    private String cardKey;
    private String cardType;
    private String algorithmVersion;
    private HomeCardPayload payload;
    private Long contentId;
    private String contentType;
    private String title;
    private String summary;
    private String coverUrl;
    private AuthorVO author;
    private List<RelationTagVO> relationTags;
    private Integer likeCount;
    private Integer commentCount;
    private Integer favoriteCount;
    private HotCommentVO hotComment;
    private Boolean liked;
    private Boolean favorited;
    private LocalDateTime publishTime;
    private Long matchId;
    private Long leagueId;
    private String leagueName;
    private MatchTeamVO homeTeam;
    private MatchTeamVO awayTeam;
    private Integer homeScore;
    private Integer awayScore;
    private String matchStatus;
    private LocalDateTime matchTime;
    private String eventSummary;
    private Boolean hasReport;
    private Long reportContentId;
    private Double score;
    private String reasonCode;
    private String reason;
    private String impressionId;
    private Integer position;
    @JsonIgnore
    private Double recommendationHotScore;
    @JsonIgnore
    private Integer recommendationImportantLevel;

    public String getCardId() { return cardId; }
    public void setCardId(String cardId) { this.cardId = cardId; }
    public String getCardKey() { return cardKey; }
    public void setCardKey(String cardKey) { this.cardKey = cardKey; }
    public String getCardType() { return cardType; }
    public void setCardType(String cardType) { this.cardType = cardType; }
    public String getAlgorithmVersion() { return algorithmVersion; }
    public void setAlgorithmVersion(String value) { algorithmVersion = value; }
    public HomeCardPayload getPayload() { return payload; }
    public void setPayload(HomeCardPayload value) { payload = value; }
    public Long getContentId() { return contentId; }
    public void setContentId(Long contentId) { this.contentId = contentId; }
    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    public String getCoverUrl() { return coverUrl; }
    public void setCoverUrl(String coverUrl) { this.coverUrl = coverUrl; }
    public AuthorVO getAuthor() { return author; }
    public void setAuthor(AuthorVO author) { this.author = author; }
    public List<RelationTagVO> getRelationTags() { return relationTags; }
    public void setRelationTags(List<RelationTagVO> relationTags) { this.relationTags = relationTags; }
    public Integer getLikeCount() { return likeCount; }
    public void setLikeCount(Integer likeCount) { this.likeCount = likeCount; }
    public Integer getCommentCount() { return commentCount; }
    public void setCommentCount(Integer commentCount) { this.commentCount = commentCount; }
    public Integer getFavoriteCount() { return favoriteCount; }
    public void setFavoriteCount(Integer favoriteCount) { this.favoriteCount = favoriteCount; }
    public HotCommentVO getHotComment() { return hotComment; }
    public void setHotComment(HotCommentVO hotComment) { this.hotComment = hotComment; }
    public Boolean getLiked() { return liked; }
    public void setLiked(Boolean liked) { this.liked = liked; }
    public Boolean getFavorited() { return favorited; }
    public void setFavorited(Boolean favorited) { this.favorited = favorited; }
    public LocalDateTime getPublishTime() { return publishTime; }
    public void setPublishTime(LocalDateTime publishTime) { this.publishTime = publishTime; }
    public Long getMatchId() { return matchId; }
    public void setMatchId(Long matchId) { this.matchId = matchId; }
    public Long getLeagueId() { return leagueId; }
    public void setLeagueId(Long leagueId) { this.leagueId = leagueId; }
    public String getLeagueName() { return leagueName; }
    public void setLeagueName(String leagueName) { this.leagueName = leagueName; }
    public MatchTeamVO getHomeTeam() { return homeTeam; }
    public void setHomeTeam(MatchTeamVO homeTeam) { this.homeTeam = homeTeam; }
    public MatchTeamVO getAwayTeam() { return awayTeam; }
    public void setAwayTeam(MatchTeamVO awayTeam) { this.awayTeam = awayTeam; }
    public Integer getHomeScore() { return homeScore; }
    public void setHomeScore(Integer homeScore) { this.homeScore = homeScore; }
    public Integer getAwayScore() { return awayScore; }
    public void setAwayScore(Integer awayScore) { this.awayScore = awayScore; }
    public String getMatchStatus() { return matchStatus; }
    public void setMatchStatus(String matchStatus) { this.matchStatus = matchStatus; }
    public LocalDateTime getMatchTime() { return matchTime; }
    public void setMatchTime(LocalDateTime matchTime) { this.matchTime = matchTime; }
    public String getEventSummary() { return eventSummary; }
    public void setEventSummary(String eventSummary) { this.eventSummary = eventSummary; }
    public Boolean getHasReport() { return hasReport; }
    public void setHasReport(Boolean hasReport) { this.hasReport = hasReport; }
    public Long getReportContentId() { return reportContentId; }
    public void setReportContentId(Long reportContentId) { this.reportContentId = reportContentId; }
    public Double getScore() { return score; }
    public void setScore(Double score) { this.score = score; }
    public String getReasonCode() { return reasonCode; }
    public void setReasonCode(String reasonCode) { this.reasonCode = reasonCode; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public String getImpressionId() { return impressionId; }
    public void setImpressionId(String impressionId) { this.impressionId = impressionId; }
    public Integer getPosition() { return position; }
    public void setPosition(Integer position) { this.position = position; }
    public Double getRecommendationHotScore() { return recommendationHotScore; }
    public void setRecommendationHotScore(Double value) { recommendationHotScore = value; }
    public Integer getRecommendationImportantLevel() { return recommendationImportantLevel; }
    public void setRecommendationImportantLevel(Integer value) { recommendationImportantLevel = value; }
}
