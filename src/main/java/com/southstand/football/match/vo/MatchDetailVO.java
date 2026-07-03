package com.southstand.football.match.vo;

import com.southstand.football.event.vo.MatchEventVO;
import com.southstand.football.report.vo.MatchReportVO;
import java.time.LocalDateTime;
import java.util.List;

public class MatchDetailVO {

    private Long matchId;
    private Long leagueId;
    private String leagueName;
    private String season;
    private String roundName;
    private String venue;
    private MatchTeamVO homeTeam;
    private MatchTeamVO awayTeam;
    private String matchStatus;
    private LocalDateTime matchTime;
    private List<MatchEventVO> eventList;
    private MatchReportVO report;

    public Long getMatchId() { return matchId; }
    public void setMatchId(Long matchId) { this.matchId = matchId; }
    public Long getLeagueId() { return leagueId; }
    public void setLeagueId(Long leagueId) { this.leagueId = leagueId; }
    public String getLeagueName() { return leagueName; }
    public void setLeagueName(String leagueName) { this.leagueName = leagueName; }
    public String getSeason() { return season; }
    public void setSeason(String season) { this.season = season; }
    public String getRoundName() { return roundName; }
    public void setRoundName(String roundName) { this.roundName = roundName; }
    public String getVenue() { return venue; }
    public void setVenue(String venue) { this.venue = venue; }
    public MatchTeamVO getHomeTeam() { return homeTeam; }
    public void setHomeTeam(MatchTeamVO homeTeam) { this.homeTeam = homeTeam; }
    public MatchTeamVO getAwayTeam() { return awayTeam; }
    public void setAwayTeam(MatchTeamVO awayTeam) { this.awayTeam = awayTeam; }
    public String getMatchStatus() { return matchStatus; }
    public void setMatchStatus(String matchStatus) { this.matchStatus = matchStatus; }
    public LocalDateTime getMatchTime() { return matchTime; }
    public void setMatchTime(LocalDateTime matchTime) { this.matchTime = matchTime; }
    public List<MatchEventVO> getEventList() { return eventList; }
    public void setEventList(List<MatchEventVO> eventList) { this.eventList = eventList; }
    public MatchReportVO getReport() { return report; }
    public void setReport(MatchReportVO report) { this.report = report; }
}
