package com.southstand.card.vo;

import com.southstand.football.match.vo.MatchTeamVO;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record PlayerRatingCardPayload(Long matchId, Long leagueId, String leagueName,
                                      LocalDateTime matchTime, MatchTeamVO homeTeam,
                                      MatchTeamVO awayTeam, Integer homeScore, Integer awayScore,
                                      List<RatingPlayer> topPlayers, int ratingUserCount)
        implements HomeCardPayload {
    public record RatingPlayer(Long playerId, String playerName, String avatarUrl, Long teamId,
                               BigDecimal officialRating, BigDecimal userRatingAverage,
                               int userRatingCount) { }
}
