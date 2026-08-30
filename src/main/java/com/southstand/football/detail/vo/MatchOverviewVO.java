package com.southstand.football.detail.vo;

import com.southstand.common.result.PageResult;
import com.southstand.football.match.vo.MatchDetailVO;
import com.southstand.football.matchdata.vo.MatchDataVO;
import java.util.List;

public record MatchOverviewVO(
        MatchDetailVO match,
        MatchDataVO.Lineups lineups,
        List<MatchDataVO.TeamStatItem> teamStats,
        PageResult<MatchDataVO.PlayerStat> playerStats,
        List<MatchDataVO.RatingSummary> ratings,
        Ranking ranking
) {
    public record Ranking(Long leagueId, String leagueName, Long seasonId, String seasonName,
                          Long stageId, String stageName, String snapshotType,
                          StandingSnapshot home, StandingSnapshot away) { }

    public record StandingSnapshot(Long teamId, String teamName, Integer rank, Integer played,
                                   Integer won, Integer drawn, Integer lost, Integer goalsFor,
                                   Integer goalsAgainst, Integer goalDifference, Integer points) { }
}
