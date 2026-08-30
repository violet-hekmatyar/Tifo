package com.southstand.football.detail.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.southstand.common.enums.ErrorCode;
import com.southstand.common.exception.BusinessException;
import com.southstand.football.detail.vo.MatchOverviewVO;
import com.southstand.football.detail.vo.MatchOverviewVO.StandingSnapshot;
import com.southstand.football.league.entity.FootballLeague;
import com.southstand.football.league.mapper.FootballLeagueMapper;
import com.southstand.football.match.entity.MatchInfo;
import com.southstand.football.match.mapper.MatchInfoMapper;
import com.southstand.football.match.vo.MatchDetailVO;
import com.southstand.football.matchdata.service.MatchDataService;
import com.southstand.football.rank.entity.FootballCompetitionStage;
import com.southstand.football.rank.entity.FootballSeason;
import com.southstand.football.rank.entity.FootballStanding;
import com.southstand.football.rank.mapper.FootballCompetitionStageMapper;
import com.southstand.football.rank.mapper.FootballSeasonMapper;
import com.southstand.football.rank.mapper.FootballStandingMapper;
import com.southstand.football.schedule.service.FootballQueryService;
import com.southstand.football.team.entity.FootballTeam;
import com.southstand.football.team.mapper.FootballTeamMapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class MatchOverviewService {
    private static final String ACTIVE = "ACTIVE";
    private final FootballQueryService footballQueryService;
    private final MatchDataService matchDataService;
    private final MatchInfoMapper matchMapper;
    private final FootballLeagueMapper leagueMapper;
    private final FootballSeasonMapper seasonMapper;
    private final FootballCompetitionStageMapper stageMapper;
    private final FootballStandingMapper standingMapper;
    private final FootballTeamMapper teamMapper;

    public MatchOverviewService(FootballQueryService footballQueryService, MatchDataService matchDataService,
                                MatchInfoMapper matchMapper, FootballLeagueMapper leagueMapper,
                                FootballSeasonMapper seasonMapper, FootballCompetitionStageMapper stageMapper,
                                FootballStandingMapper standingMapper, FootballTeamMapper teamMapper) {
        this.footballQueryService = footballQueryService; this.matchDataService = matchDataService;
        this.matchMapper = matchMapper; this.leagueMapper = leagueMapper; this.seasonMapper = seasonMapper;
        this.stageMapper = stageMapper; this.standingMapper = standingMapper; this.teamMapper = teamMapper;
    }

    public MatchOverviewVO overview(Long matchId) {
        MatchDetailVO detail = footballQueryService.matchDetail(matchId);
        matchDataService.enhance(detail);
        return new MatchOverviewVO(detail, matchDataService.lineups(matchId), matchDataService.teamStats(matchId),
                matchDataService.playerStats(matchId, null, null, 1, 100), matchDataService.ratings(matchId, null),
                ranking(matchId));
    }

    private MatchOverviewVO.Ranking ranking(Long matchId) {
        MatchInfo match = matchMapper.selectById(matchId);
        if (match == null || !ACTIVE.equals(match.getStatus()) || !Objects.equals(match.getIsDeleted(), 0)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "match not found");
        }
        FootballLeague league = leagueMapper.selectById(match.getLeagueId());
        QueryWrapper<FootballSeason> seasonQuery = new QueryWrapper<FootballSeason>()
                .eq("league_id", match.getLeagueId()).eq("status", ACTIVE).eq("is_deleted", 0);
        if (StringUtils.hasText(match.getSeason())) {
            seasonQuery.and(w -> w.eq("season_name", match.getSeason()).or().eq("season_code", match.getSeason()));
        }
        seasonQuery.orderByDesc("current_flag").orderByDesc("start_date").orderByDesc("id").last("LIMIT 1");
        FootballSeason season = seasonMapper.selectOne(seasonQuery);
        if (season == null && StringUtils.hasText(match.getSeason())) {
            season = seasonMapper.selectOne(new QueryWrapper<FootballSeason>().eq("league_id", match.getLeagueId())
                    .eq("status", ACTIVE).eq("is_deleted", 0).orderByDesc("current_flag")
                    .orderByDesc("start_date").orderByDesc("id").last("LIMIT 1"));
        }
        if (season == null) return new MatchOverviewVO.Ranking(match.getLeagueId(), leagueName(league), null, null,
                null, null, "UNAVAILABLE", null, null);
        FootballCompetitionStage stage = stageMapper.selectOne(new QueryWrapper<FootballCompetitionStage>()
                .eq("league_id", match.getLeagueId()).eq("season_id", season.getId()).eq("status", ACTIVE)
                .eq("is_deleted", 0).orderByAsc("sort_order").orderByAsc("id").last("LIMIT 1"));
        QueryWrapper<FootballStanding> standingQuery = new QueryWrapper<FootballStanding>()
                .eq("league_id", match.getLeagueId()).eq("season_id", season.getId())
                .in("team_id", List.of(match.getHomeTeamId(), match.getAwayTeamId())).eq("is_deleted", 0);
        if (stage != null) standingQuery.eq("stage_id", stage.getId());
        List<FootballStanding> rows = standingMapper.selectList(standingQuery.orderByAsc("team_id"));
        Map<Long, FootballStanding> standingMap = rows.stream().collect(Collectors.toMap(
                FootballStanding::getTeamId, Function.identity(), (a, b) -> a, LinkedHashMap::new));
        Set<Long> teamIds = Set.of(match.getHomeTeamId(), match.getAwayTeamId());
        Map<Long, FootballTeam> teams = teamMapper.selectBatchIds(teamIds).stream().collect(Collectors.toMap(
                FootballTeam::getId, Function.identity(), (a, b) -> a, LinkedHashMap::new));
        return new MatchOverviewVO.Ranking(match.getLeagueId(), leagueName(league), season.getId(), season.getSeasonName(),
                stage == null ? null : stage.getId(), stage == null ? null : stage.getStageName(), "CURRENT_STANDING",
                snapshot(match.getHomeTeamId(), teams, standingMap), snapshot(match.getAwayTeamId(), teams, standingMap));
    }

    private StandingSnapshot snapshot(Long teamId, Map<Long, FootballTeam> teams, Map<Long, FootballStanding> standings) {
        FootballTeam team = teams.get(teamId); FootballStanding s = standings.get(teamId);
        return new StandingSnapshot(teamId, team == null ? null : team.getTeamName(), s == null ? null : s.getRankNo(),
                s == null ? null : s.getPlayed(), s == null ? null : s.getWon(), s == null ? null : s.getDrawn(),
                s == null ? null : s.getLost(), s == null ? null : s.getGoalsFor(), s == null ? null : s.getGoalsAgainst(),
                s == null ? null : s.getGoalDifference(), s == null ? null : s.getPoints());
    }

    private String leagueName(FootballLeague league) { return league == null ? null : league.getLeagueName(); }
}
