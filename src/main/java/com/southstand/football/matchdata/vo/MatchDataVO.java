package com.southstand.football.matchdata.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public final class MatchDataVO {
    private MatchDataVO(){}
    public record Lineups(TeamLineup home,TeamLineup away){}
    public record TeamLineup(Long teamId,String teamName,String teamLogoUrl,String formation,String coachName,List<LineupPlayer> starters,List<LineupPlayer> substitutes,List<LineupPlayer> bench){}
    public record LineupPlayer(Long playerId,String playerName,String avatarUrl,String position,Integer shirtNumber,Boolean captain,Boolean started,Boolean appeared,Integer substitutedInMinute,Integer substitutedOutMinute,BigDecimal fieldX,BigDecimal fieldY){}
    public record TeamStatItem(String statType,String displayName,Object homeValue,Object awayValue,String unit){}
    public record PlayerStat(Long playerId,String playerName,String avatarUrl,Long teamId,String teamName,String position,Integer shirtNumber,Boolean starter,Boolean captain,Integer minutes,Integer goals,Integer assists,Integer shots,Integer shotsOnTarget,Integer passes,Integer successfulPasses,BigDecimal passAccuracy,Integer keyPasses,Integer tackles,Integer interceptions,Integer saves,Integer yellowCards,Integer redCards,BigDecimal officialRating,BigDecimal userRatingAverage,Integer userRatingCount,BigDecimal currentUserRating){}
    public record RatingRequest(BigDecimal rating){}
    public record RatingResult(Long matchId,Long playerId,BigDecimal myRating,BigDecimal averageRating,Integer ratingCount,LocalDateTime updatedAt){}
    public record RatingSummary(Long playerId,String playerName,String avatarUrl,Long teamId,BigDecimal officialRating,BigDecimal averageRating,Integer ratingCount,BigDecimal currentUserRating,Map<String,Integer> distribution,Long ratingTargetId){}
    public record ManOfTheMatch(Long playerId,String playerName,Long teamId,BigDecimal rating,String ratingSource,Integer ratingCount){}
}
