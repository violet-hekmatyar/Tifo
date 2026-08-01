package com.southstand.football.rank.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.southstand.common.enums.ErrorCode;
import com.southstand.common.exception.BusinessException;
import com.southstand.common.result.PageResult;
import com.southstand.football.league.entity.FootballLeague;
import com.southstand.football.league.mapper.FootballLeagueMapper;
import com.southstand.football.player.entity.FootballPlayer;
import com.southstand.football.player.mapper.FootballPlayerMapper;
import com.southstand.football.rank.entity.FootballCompetitionStage;
import com.southstand.football.rank.entity.FootballPlayerCompetitionStat;
import com.southstand.football.rank.entity.FootballSeason;
import com.southstand.football.rank.entity.FootballStanding;
import com.southstand.football.rank.entity.FootballTeamCompetitionStat;
import com.southstand.football.rank.enums.PlayerRankType;
import com.southstand.football.rank.enums.TeamRankType;
import com.southstand.football.rank.mapper.FootballCompetitionStageMapper;
import com.southstand.football.rank.mapper.FootballPlayerCompetitionStatMapper;
import com.southstand.football.rank.mapper.FootballSeasonMapper;
import com.southstand.football.rank.mapper.FootballStandingMapper;
import com.southstand.football.rank.mapper.FootballTeamCompetitionStatMapper;
import com.southstand.football.rank.vo.PlayerRankRecordVO;
import com.southstand.football.rank.vo.SeasonVO;
import com.southstand.football.rank.vo.StageVO;
import com.southstand.football.rank.vo.StandingRecordVO;
import com.southstand.football.rank.vo.StandingTableVO;
import com.southstand.football.rank.vo.TeamRankRecordVO;
import com.southstand.football.team.entity.FootballTeam;
import com.southstand.football.team.mapper.FootballTeamMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Collections;
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
public class FootballRankService {
    private static final String ACTIVE = "ACTIVE";
    private static final int NOT_DELETED = 0;

    private final FootballLeagueMapper leagueMapper;
    private final FootballSeasonMapper seasonMapper;
    private final FootballCompetitionStageMapper stageMapper;
    private final FootballStandingMapper standingMapper;
    private final FootballPlayerCompetitionStatMapper playerStatMapper;
    private final FootballTeamCompetitionStatMapper teamStatMapper;
    private final FootballPlayerMapper playerMapper;
    private final FootballTeamMapper teamMapper;

    public FootballRankService(FootballLeagueMapper leagueMapper, FootballSeasonMapper seasonMapper,
            FootballCompetitionStageMapper stageMapper, FootballStandingMapper standingMapper,
            FootballPlayerCompetitionStatMapper playerStatMapper,
            FootballTeamCompetitionStatMapper teamStatMapper, FootballPlayerMapper playerMapper,
            FootballTeamMapper teamMapper) {
        this.leagueMapper=leagueMapper; this.seasonMapper=seasonMapper; this.stageMapper=stageMapper;
        this.standingMapper=standingMapper; this.playerStatMapper=playerStatMapper;
        this.teamStatMapper=teamStatMapper; this.playerMapper=playerMapper; this.teamMapper=teamMapper;
    }

    public List<SeasonVO> seasons(Long leagueId) {
        requireLeague(leagueId);
        return safe(seasonMapper.selectList(new QueryWrapper<FootballSeason>()
                .eq("league_id", leagueId).eq("status", ACTIVE).eq("is_deleted", NOT_DELETED)
                .orderByDesc("current_flag").orderByDesc("start_date").orderByDesc("id")))
                .stream().map(this::seasonVO).toList();
    }

    public List<StageVO> stages(Long leagueId, Long seasonId) {
        requireSeason(leagueId, seasonId);
        return safe(stageMapper.selectList(new QueryWrapper<FootballCompetitionStage>()
                .eq("league_id", leagueId).eq("season_id", seasonId)
                .eq("status", ACTIVE).eq("is_deleted", NOT_DELETED)
                .orderByAsc("sort_order").orderByAsc("id")))
                .stream().map(this::stageVO).toList();
    }

    public StandingTableVO standings(Long leagueId, Long seasonId, Long stageId, String groupCode) {
        FootballLeague league = requireLeague(leagueId);
        FootballSeason season = requireSeason(leagueId, seasonId);
        FootballCompetitionStage stage = resolveStage(leagueId, seasonId, stageId);
        QueryWrapper<FootballStanding> query = new QueryWrapper<FootballStanding>()
                .eq("league_id", leagueId).eq("season_id", seasonId).eq("stage_id", stage.getId())
                .eq("is_deleted", NOT_DELETED).orderByAsc("rank_no");
        if (StringUtils.hasText(groupCode)) { query.eq("group_code", groupCode); }
        List<FootballStanding> rows = safe(standingMapper.selectList(query));
        Map<Long, FootballTeam> teams = teams(rows.stream().map(FootballStanding::getTeamId).collect(Collectors.toSet()));
        List<StandingRecordVO> records = rows.stream().map(row -> standingVO(row, teams.get(row.getTeamId()))).toList();
        LocalDateTime updatedAt = rows.stream().map(FootballStanding::getSourceUpdatedAt).filter(Objects::nonNull)
                .max(LocalDateTime::compareTo).orElse(null);
        String source = rows.isEmpty() ? season.getSource() : rows.get(0).getSource();
        return new StandingTableVO(leagueId, league.getLeagueName(), seasonId, season.getSeasonName(),
                stage.getId(), stage.getStageName(), groupCode, source, updatedAt, records);
    }

    public PageResult<PlayerRankRecordVO> playerRanks(Long leagueId, Long seasonId, Long stageId,
            String rankTypeValue, long pageNum, long pageSize) {
        requireSeason(leagueId, seasonId);
        FootballCompetitionStage stage = resolveStage(leagueId, seasonId, stageId);
        PlayerRankType type = PlayerRankType.parse(rankTypeValue);
        long pn = normalizePageNum(pageNum), ps = normalizePageSize(pageSize);
        QueryWrapper<FootballPlayerCompetitionStat> scope = playerScope(leagueId, seasonId, stage.getId());
        Long count = playerStatMapper.selectCount(scope);
        QueryWrapper<FootballPlayerCompetitionStat> query = playerScope(leagueId, seasonId, stage.getId());
        applyPlayerOrder(query, type);
        query.last("LIMIT " + ((pn - 1) * ps) + "," + ps);
        List<FootballPlayerCompetitionStat> rows = safe(playerStatMapper.selectList(query));
        Map<Long, FootballPlayer> players = players(rows.stream().map(FootballPlayerCompetitionStat::getPlayerId).collect(Collectors.toSet()));
        Map<Long, FootballTeam> teams = teams(rows.stream().map(FootballPlayerCompetitionStat::getTeamId).collect(Collectors.toSet()));
        int offset = Math.toIntExact((pn - 1) * ps);
        List<PlayerRankRecordVO> records = java.util.stream.IntStream.range(0, rows.size())
                .mapToObj(i -> playerRankVO(offset + i + 1, rows.get(i), type,
                        players.get(rows.get(i).getPlayerId()), teams.get(rows.get(i).getTeamId()))).toList();
        return PageResult.of(records, count == null ? 0 : count, pn, ps);
    }

    public PageResult<TeamRankRecordVO> teamRanks(Long leagueId, Long seasonId, Long stageId,
            String rankTypeValue, long pageNum, long pageSize) {
        requireSeason(leagueId, seasonId);
        FootballCompetitionStage stage = resolveStage(leagueId, seasonId, stageId);
        TeamRankType type = TeamRankType.parse(rankTypeValue);
        long pn = normalizePageNum(pageNum), ps = normalizePageSize(pageSize);
        QueryWrapper<FootballTeamCompetitionStat> scope = teamScope(leagueId, seasonId, stage.getId());
        Long count = teamStatMapper.selectCount(scope);
        QueryWrapper<FootballTeamCompetitionStat> query = teamScope(leagueId, seasonId, stage.getId());
        query.orderBy(true, type.ascending(), type.column()).orderByAsc("team_id")
                .last("LIMIT " + ((pn - 1) * ps) + "," + ps);
        List<FootballTeamCompetitionStat> rows = safe(teamStatMapper.selectList(query));
        Map<Long, FootballTeam> teams = teams(rows.stream().map(FootballTeamCompetitionStat::getTeamId).collect(Collectors.toSet()));
        int offset = Math.toIntExact((pn - 1) * ps);
        List<TeamRankRecordVO> records = java.util.stream.IntStream.range(0, rows.size())
                .mapToObj(i -> teamRankVO(offset + i + 1, rows.get(i), type, teams.get(rows.get(i).getTeamId()))).toList();
        return PageResult.of(records, count == null ? 0 : count, pn, ps);
    }

    private void applyPlayerOrder(QueryWrapper<FootballPlayerCompetitionStat> query, PlayerRankType type) {
        query.orderBy(true, type.ascending(), type.column());
        switch (type) {
            case GOALS -> query.orderByDesc("assists").orderByAsc("appearances");
            case ASSISTS -> query.orderByDesc("goals").orderByAsc("appearances");
            case YELLOW_CARDS, RED_CARDS -> query.orderByAsc("appearances");
            case SHOTS, SHOTS_ON_TARGET -> query.orderByDesc("goals");
            case RATING, SAVES -> query.orderByDesc("appearances");
            case APPEARANCES -> query.orderByDesc("minutes");
            case MINUTES -> query.orderByDesc("appearances");
        }
        query.orderByAsc("player_id");
    }

    private QueryWrapper<FootballPlayerCompetitionStat> playerScope(Long leagueId, Long seasonId, Long stageId) {
        return new QueryWrapper<FootballPlayerCompetitionStat>().eq("league_id", leagueId)
                .eq("season_id", seasonId).eq("stage_id", stageId).eq("is_deleted", NOT_DELETED);
    }
    private QueryWrapper<FootballTeamCompetitionStat> teamScope(Long leagueId, Long seasonId, Long stageId) {
        return new QueryWrapper<FootballTeamCompetitionStat>().eq("league_id", leagueId)
                .eq("season_id", seasonId).eq("stage_id", stageId).eq("is_deleted", NOT_DELETED);
    }

    private FootballLeague requireLeague(Long leagueId) {
        FootballLeague league = leagueMapper.selectById(leagueId);
        if (league == null || !ACTIVE.equals(league.getStatus()) || !Objects.equals(league.getIsDeleted(), NOT_DELETED))
            throw new BusinessException(ErrorCode.NOT_FOUND, "league not found");
        return league;
    }
    private FootballSeason requireSeason(Long leagueId, Long seasonId) {
        FootballSeason season = seasonMapper.selectById(seasonId);
        if (season == null || !Objects.equals(season.getLeagueId(), leagueId) || !ACTIVE.equals(season.getStatus())
                || !Objects.equals(season.getIsDeleted(), NOT_DELETED))
            throw new BusinessException(ErrorCode.NOT_FOUND, "season not found for league");
        return season;
    }
    private FootballCompetitionStage resolveStage(Long leagueId, Long seasonId, Long stageId) {
        FootballCompetitionStage stage;
        if (stageId != null) { stage = stageMapper.selectById(stageId); }
        else { stage = stageMapper.selectOne(new QueryWrapper<FootballCompetitionStage>()
                .eq("league_id", leagueId).eq("season_id", seasonId).eq("status", ACTIVE)
                .eq("is_deleted", NOT_DELETED).orderByAsc("sort_order").orderByAsc("id").last("LIMIT 1")); }
        if (stage == null || !Objects.equals(stage.getLeagueId(), leagueId) || !Objects.equals(stage.getSeasonId(), seasonId)
                || !ACTIVE.equals(stage.getStatus()) || !Objects.equals(stage.getIsDeleted(), NOT_DELETED))
            throw new BusinessException(ErrorCode.NOT_FOUND, "competition stage not found");
        return stage;
    }

    private SeasonVO seasonVO(FootballSeason row) { return new SeasonVO(row.getId(), row.getLeagueId(), row.getSeasonCode(),
            row.getSeasonName(), row.getStartDate(), row.getEndDate(), Objects.equals(row.getCurrentFlag(), 1), row.getStatus()); }
    private StageVO stageVO(FootballCompetitionStage row) { return new StageVO(row.getId(), row.getStageType(), row.getStageName(), row.getGroupCode(), row.getSortOrder()); }
    private StandingRecordVO standingVO(FootballStanding row, FootballTeam team) { return new StandingRecordVO(row.getRankNo(), row.getTeamId(),
            team == null ? null : team.getTeamName(), team == null ? null : team.getLogoUrl(), row.getPlayed(), row.getWon(), row.getDrawn(), row.getLost(),
            row.getGoalsFor(), row.getGoalsAgainst(), row.getGoalDifference(), row.getPoints(), row.getDeductionPoints(), row.getFormText()); }
    private PlayerRankRecordVO playerRankVO(int rank, FootballPlayerCompetitionStat row, PlayerRankType type, FootballPlayer player, FootballTeam team) {
        BigDecimal value = playerValue(row, type); return new PlayerRankRecordVO(rank, row.getPlayerId(), player == null ? null : player.getPlayerName(),
                player == null ? null : player.getAvatarUrl(), row.getTeamId(), team == null ? null : team.getTeamName(), team == null ? null : team.getLogoUrl(),
                value, display(value, type == PlayerRankType.RATING), row.getAppearances(), row.getStarts(), row.getMinutes(), row.getSourceUpdatedAt()); }
    private TeamRankRecordVO teamRankVO(int rank, FootballTeamCompetitionStat row, TeamRankType type, FootballTeam team) {
        BigDecimal value = teamValue(row, type); return new TeamRankRecordVO(rank, row.getTeamId(), team == null ? null : team.getTeamName(),
                team == null ? null : team.getLogoUrl(), value, display(value, type == TeamRankType.AVG_RATING), row.getPlayed(), type.direction(), row.getSourceUpdatedAt()); }
    private BigDecimal playerValue(FootballPlayerCompetitionStat r, PlayerRankType t) { return switch(t) {
        case GOALS -> bd(r.getGoals()); case ASSISTS -> bd(r.getAssists()); case YELLOW_CARDS -> bd(r.getYellowCards());
        case RED_CARDS -> bd(r.getRedCards()); case SHOTS -> bd(r.getShots()); case SHOTS_ON_TARGET -> bd(r.getShotsOnTarget());
        case RATING -> r.getRating(); case SAVES -> bd(r.getSaves()); case APPEARANCES -> bd(r.getAppearances()); case MINUTES -> bd(r.getMinutes()); }; }
    private BigDecimal teamValue(FootballTeamCompetitionStat r, TeamRankType t) { return switch(t) {
        case GOALS_FOR -> bd(r.getGoalsFor()); case GOALS_AGAINST -> bd(r.getGoalsAgainst()); case ASSISTS -> bd(r.getAssists());
        case YELLOW_CARDS -> bd(r.getYellowCards()); case RED_CARDS -> bd(r.getRedCards()); case SHOTS -> bd(r.getShots());
        case SHOTS_ON_TARGET -> bd(r.getShotsOnTarget()); case CORNERS -> bd(r.getCorners()); case FOULS -> bd(r.getFouls());
        case CLEAN_SHEETS -> bd(r.getCleanSheets()); case AVG_RATING -> r.getAvgRating(); }; }
    private BigDecimal bd(Integer value) { return BigDecimal.valueOf(value == null ? 0 : value); }
    private String display(BigDecimal value, boolean decimal) { if (value == null) return null;
        return decimal ? value.setScale(2, RoundingMode.HALF_UP).toPlainString() : value.setScale(0, RoundingMode.DOWN).toPlainString(); }
    private Map<Long, FootballTeam> teams(Set<Long> ids) { return ids.isEmpty() ? Collections.emptyMap() : index(safe(teamMapper.selectBatchIds(ids)), FootballTeam::getId); }
    private Map<Long, FootballPlayer> players(Set<Long> ids) { return ids.isEmpty() ? Collections.emptyMap() : index(safe(playerMapper.selectBatchIds(ids)), FootballPlayer::getId); }
    private <T> Map<Long,T> index(List<T> rows, Function<T,Long> id) { return rows.stream().collect(Collectors.toMap(id, Function.identity(), (a,b)->a, LinkedHashMap::new)); }
    private <T> List<T> safe(List<T> rows) { return rows == null ? List.of() : rows; }
    private long normalizePageNum(long value) { return value <= 0 ? 1 : value; }
    private long normalizePageSize(long value) { return value <= 0 ? 20 : Math.min(value, 100); }
}
