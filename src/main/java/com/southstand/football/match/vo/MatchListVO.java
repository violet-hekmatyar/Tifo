package com.southstand.football.match.vo;

import java.time.LocalDateTime;

public class MatchListVO {

    private Long matchId;
    private Long leagueId;
    private String leagueName;
    private MatchTeamVO homeTeam;
    private MatchTeamVO awayTeam;
    private String matchStatus;
    private LocalDateTime matchTime;
    private String eventSummary;
    private Boolean hasReport;
    private Long reportContentId;

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
}
