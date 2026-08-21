package com.southstand.football.match.vo;

import com.southstand.football.event.vo.MatchEventVO;
import com.southstand.football.report.vo.MatchReportVO;
import com.southstand.football.matchdata.vo.MatchDataVO.ManOfTheMatch;
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
    private Boolean lineupsAvailable;
    private Boolean teamStatsAvailable;
    private Boolean playerStatsAvailable;
    private Boolean ratingsAvailable;
    private Integer ratingUserCount;
    private ManOfTheMatch manOfTheMatch;

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
    public Boolean getLineupsAvailable(){return lineupsAvailable;} public void setLineupsAvailable(Boolean v){lineupsAvailable=v;}
    public Boolean getTeamStatsAvailable(){return teamStatsAvailable;} public void setTeamStatsAvailable(Boolean v){teamStatsAvailable=v;}
    public Boolean getPlayerStatsAvailable(){return playerStatsAvailable;} public void setPlayerStatsAvailable(Boolean v){playerStatsAvailable=v;}
    public Boolean getRatingsAvailable(){return ratingsAvailable;} public void setRatingsAvailable(Boolean v){ratingsAvailable=v;}
    public Integer getRatingUserCount(){return ratingUserCount;} public void setRatingUserCount(Integer v){ratingUserCount=v;}
    public ManOfTheMatch getManOfTheMatch(){return manOfTheMatch;} public void setManOfTheMatch(ManOfTheMatch v){manOfTheMatch=v;}
}
