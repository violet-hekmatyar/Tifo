package com.southstand.football.match.vo;

import java.util.List;

public class BracketTieVO {

    private String tieKey;
    private MatchTeamVO homeTeam;
    private MatchTeamVO awayTeam;
    private Integer homeAggregate;
    private Integer awayAggregate;
    private Long winnerTeamId;
    private String parentTieKey;
    private List<BracketLegVO> legs;

    public String getTieKey() { return tieKey; }
    public void setTieKey(String tieKey) { this.tieKey = tieKey; }
    public MatchTeamVO getHomeTeam() { return homeTeam; }
    public void setHomeTeam(MatchTeamVO homeTeam) { this.homeTeam = homeTeam; }
    public MatchTeamVO getAwayTeam() { return awayTeam; }
    public void setAwayTeam(MatchTeamVO awayTeam) { this.awayTeam = awayTeam; }
    public Integer getHomeAggregate() { return homeAggregate; }
    public void setHomeAggregate(Integer homeAggregate) { this.homeAggregate = homeAggregate; }
    public Integer getAwayAggregate() { return awayAggregate; }
    public void setAwayAggregate(Integer awayAggregate) { this.awayAggregate = awayAggregate; }
    public Long getWinnerTeamId() { return winnerTeamId; }
    public void setWinnerTeamId(Long winnerTeamId) { this.winnerTeamId = winnerTeamId; }
    public String getParentTieKey() { return parentTieKey; }
    public void setParentTieKey(String parentTieKey) { this.parentTieKey = parentTieKey; }
    public List<BracketLegVO> getLegs() { return legs; }
    public void setLegs(List<BracketLegVO> legs) { this.legs = legs; }
}
