package com.southstand.football.detail.vo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public final class FootballDetailVO {
    private FootballDetailVO(){}
    public record Standing(Integer rank,Integer played,Integer won,Integer drawn,Integer lost,Integer goalsFor,Integer goalsAgainst,Integer goalDifference,Integer points){}
    public record TeamStats(Integer played,Integer goalsFor,Integer goalsAgainst,Integer goalDifference,Integer assists,Integer shots,Integer shotsOnTarget,BigDecimal shotAccuracy,Integer corners,Integer fouls,Integer yellowCards,Integer redCards,Integer cleanSheets,BigDecimal avgRating,Integer standingRank,Integer points,String source,LocalDateTime updatedAt){}
    public record RosterPlayer(Long playerId,String playerName,String playerNameEn,String avatarUrl,String position,Integer shirtNumber,Boolean captain,Boolean loan,Long loanFromTeamId,String loanFromTeamName,String squadRole,Integer appearances,Integer starts,Integer minutes,Integer goals,Integer assists,BigDecimal rating,Boolean followed){}
    public record Honor(Long honorId,String honorName,String honorType,Integer titleCount,List<Integer> winningYears,Integer latestYear){}
    public record PlayerStats(Long leagueId,String leagueName,Long seasonId,String seasonName,Long teamId,String teamName,Integer appearances,Integer starts,Integer minutes,Integer goals,Integer assists,Integer yellowCards,Integer redCards,Integer shots,Integer shotsOnTarget,BigDecimal shotAccuracy,BigDecimal rating,Integer saves,String source,LocalDateTime updatedAt){}
    public record TeamHistory(Long teamId,String teamName,String teamLogoUrl,Long seasonId,String seasonName,LocalDate startDate,LocalDate endDate,Integer shirtNumber,String position,Integer appearances,Integer goals,Integer assists,Boolean current,Boolean loan){}
    public record CareerGroup(Long id,String name,Integer appearances,Integer starts,Integer minutes,Integer goals,Integer assists,BigDecimal averageRating){}
    public record Career(Integer totalAppearances,Integer totalStarts,Integer totalMinutes,Integer totalGoals,Integer totalAssists,Integer totalYellowCards,Integer totalRedCards,Integer totalShots,Integer totalShotsOnTarget,BigDecimal averageRating,Integer totalSaves,Integer teamCount,Integer seasonCount,List<CareerGroup> bySeason,List<CareerGroup> byTeam){}
    public record Match(Long matchId,Long leagueId,Long homeTeamId,String homeTeamName,Long awayTeamId,String awayTeamName,Integer homeScore,Integer awayScore,String matchStatus,LocalDateTime matchTime){}
    public record ContentSummary(Long contentId,String contentType,String title,String summary,String coverUrl,LocalDateTime publishTime){}
    public record TeamOverview(Long teamId,String teamName,String teamNameEn,String logoUrl,Long leagueId,String leagueName,Long seasonId,String seasonName,String city,String stadium,Integer foundedYear,String description,Boolean followed,Standing standing,TeamStats seasonStats,List<RosterPlayer> topScorers,List<RosterPlayer> topAssists,List<Match> recentMatches,Match nextMatch,List<ContentSummary> recentContents){}
    public record PlayerOverview(Long playerId,String playerName,String playerNameEn,String avatarUrl,String position,String nationality,LocalDate birthDate,Integer age,Integer height,Integer weight,String preferredFoot,Long currentTeamId,String currentTeamName,String currentTeamLogoUrl,Integer shirtNumber,Boolean captain,Boolean followed,List<PlayerStats> seasonStats,Career career,List<ContentSummary> recentContents){}
}
