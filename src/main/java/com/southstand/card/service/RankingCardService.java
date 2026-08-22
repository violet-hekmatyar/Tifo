package com.southstand.card.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.southstand.card.model.HomeFeedUserContext;
import com.southstand.card.vo.FeedCardVO;
import com.southstand.card.vo.RankingCardPayload;
import com.southstand.card.vo.RankingCardPayload.RankingItem;
import com.southstand.football.league.entity.FootballLeague;
import com.southstand.football.league.mapper.FootballLeagueMapper;
import com.southstand.football.match.entity.MatchInfo;
import com.southstand.football.match.mapper.MatchInfoMapper;
import com.southstand.football.player.entity.FootballPlayer;
import com.southstand.football.player.mapper.FootballPlayerMapper;
import com.southstand.football.rank.entity.FootballCompetitionStage;
import com.southstand.football.rank.entity.FootballPlayerCompetitionStat;
import com.southstand.football.rank.entity.FootballSeason;
import com.southstand.football.rank.entity.FootballStanding;
import com.southstand.football.rank.entity.FootballTeamCompetitionStat;
import com.southstand.football.rank.mapper.FootballCompetitionStageMapper;
import com.southstand.football.rank.mapper.FootballPlayerCompetitionStatMapper;
import com.southstand.football.rank.mapper.FootballSeasonMapper;
import com.southstand.football.rank.mapper.FootballStandingMapper;
import com.southstand.football.rank.mapper.FootballTeamCompetitionStatMapper;
import com.southstand.football.team.entity.FootballTeam;
import com.southstand.football.team.mapper.FootballTeamMapper;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class RankingCardService {
    private static final String ACTIVE = "ACTIVE";
    private static final int NOT_DELETED = 0;
    private final FootballLeagueMapper leagueMapper;
    private final MatchInfoMapper matchMapper;
    private final FootballSeasonMapper seasonMapper;
    private final FootballCompetitionStageMapper stageMapper;
    private final FootballStandingMapper standingMapper;
    private final FootballPlayerCompetitionStatMapper playerStatMapper;
    private final FootballTeamCompetitionStatMapper teamStatMapper;
    private final FootballTeamMapper teamMapper;
    private final FootballPlayerMapper playerMapper;

    public RankingCardService(FootballLeagueMapper leagueMapper, MatchInfoMapper matchMapper,
                              FootballSeasonMapper seasonMapper, FootballCompetitionStageMapper stageMapper,
                              FootballStandingMapper standingMapper,
                              FootballPlayerCompetitionStatMapper playerStatMapper,
                              FootballTeamCompetitionStatMapper teamStatMapper,
                              FootballTeamMapper teamMapper, FootballPlayerMapper playerMapper) {
        this.leagueMapper = leagueMapper; this.matchMapper = matchMapper; this.seasonMapper = seasonMapper;
        this.stageMapper = stageMapper; this.standingMapper = standingMapper;
        this.playerStatMapper = playerStatMapper; this.teamStatMapper = teamStatMapper;
        this.teamMapper = teamMapper; this.playerMapper = playerMapper;
    }

    public List<FeedCardVO> candidates(HomeFeedUserContext user) {
        FootballLeague league = chooseLeague(user);
        if (league == null) return List.of();
        FootballSeason season = seasonMapper.selectOne(new QueryWrapper<FootballSeason>()
                .eq("league_id", league.getId()).eq("status", ACTIVE).eq("is_deleted", NOT_DELETED)
                .orderByDesc("current_flag").orderByDesc("start_date").orderByDesc("id").last("LIMIT 1"));
        if (season == null) return List.of();
        FootballCompetitionStage stage = stageMapper.selectOne(new QueryWrapper<FootballCompetitionStage>()
                .eq("league_id", league.getId()).eq("season_id", season.getId())
                .eq("status", ACTIVE).eq("is_deleted", NOT_DELETED)
                .orderByAsc("sort_order").orderByAsc("id").last("LIMIT 1"));
        if (stage == null) return List.of();
        List<FootballStanding> standings = safe(standingMapper.selectList(new QueryWrapper<FootballStanding>()
                .eq("league_id", league.getId()).eq("season_id", season.getId()).eq("stage_id", stage.getId())
                .eq("is_deleted", NOT_DELETED).orderByAsc("rank_no").orderByAsc("team_id").last("LIMIT 5")));
        List<FootballPlayerCompetitionStat> players = safe(playerStatMapper.selectList(new QueryWrapper<FootballPlayerCompetitionStat>()
                .eq("league_id", league.getId()).eq("season_id", season.getId()).eq("stage_id", stage.getId())
                .eq("is_deleted", NOT_DELETED).orderByDesc("goals").orderByDesc("assists")
                .orderByAsc("appearances").orderByAsc("player_id").last("LIMIT 5")));
        List<FootballTeamCompetitionStat> teams = safe(teamStatMapper.selectList(new QueryWrapper<FootballTeamCompetitionStat>()
                .eq("league_id", league.getId()).eq("season_id", season.getId()).eq("stage_id", stage.getId())
                .eq("is_deleted", NOT_DELETED).orderByDesc("goals_for").orderByAsc("team_id").last("LIMIT 5")));
        Set<Long> teamIds = new LinkedHashSet<>(); standings.forEach(x -> teamIds.add(x.getTeamId()));
        players.forEach(x -> teamIds.add(x.getTeamId())); teams.forEach(x -> teamIds.add(x.getTeamId()));
        Map<Long, FootballTeam> teamMap = teamIds.isEmpty() ? Map.of() : safe(teamMapper.selectBatchIds(teamIds)).stream()
                .collect(Collectors.toMap(FootballTeam::getId, Function.identity(), (a, b) -> a, LinkedHashMap::new));
        Set<Long> playerIds = players.stream().map(FootballPlayerCompetitionStat::getPlayerId).collect(Collectors.toSet());
        Map<Long, FootballPlayer> playerMap = playerIds.isEmpty() ? Map.of() : safe(playerMapper.selectBatchIds(playerIds)).stream()
                .collect(Collectors.toMap(FootballPlayer::getId, Function.identity(), (a, b) -> a, LinkedHashMap::new));
        List<FeedCardVO> cards = new ArrayList<>();
        if (!standings.isEmpty()) cards.add(standingCard(league, season, standings, teamMap));
        if (!players.isEmpty()) cards.add(playerCard(league, season, players, playerMap, teamMap));
        if (!teams.isEmpty()) cards.add(teamCard(league, season, teams, teamMap));
        return cards;
    }

    private FootballLeague chooseLeague(HomeFeedUserContext user) {
        Set<Long> preferredTeams = new LinkedHashSet<>();
        if (user.mainTeamId() != null) preferredTeams.add(user.mainTeamId());
        preferredTeams.addAll(user.followedTeamIds());
        if (!preferredTeams.isEmpty()) {
            List<MatchInfo> matches = safe(matchMapper.selectList(new QueryWrapper<MatchInfo>()
                    .and(w -> w.in("home_team_id", preferredTeams).or().in("away_team_id", preferredTeams))
                    .eq("status", ACTIVE).eq("is_deleted", NOT_DELETED)
                    .orderByDesc("match_time").orderByAsc("id").last("LIMIT 100")));
            Long mainLeague = matches.stream().filter(m -> user.mainTeamId() != null
                            && (Objects.equals(m.getHomeTeamId(), user.mainTeamId()) || Objects.equals(m.getAwayTeamId(), user.mainTeamId())))
                    .map(MatchInfo::getLeagueId).findFirst().orElse(null);
            Long leagueId = mainLeague != null ? mainLeague : matches.stream().map(MatchInfo::getLeagueId).findFirst().orElse(null);
            if (leagueId != null) {
                FootballLeague selected = leagueMapper.selectById(leagueId);
                if (selected != null && ACTIVE.equals(selected.getStatus()) && Objects.equals(selected.getIsDeleted(), NOT_DELETED)) return selected;
            }
        }
        return leagueMapper.selectOne(new QueryWrapper<FootballLeague>().eq("status", ACTIVE)
                .eq("is_deleted", NOT_DELETED).orderByAsc("sort_order").orderByAsc("id").last("LIMIT 1"));
    }

    private FeedCardVO standingCard(FootballLeague l, FootballSeason s, List<FootballStanding> rows, Map<Long, FootballTeam> teams) {
        List<RankingItem> items = rows.stream().map(r -> { FootballTeam t = teams.get(r.getTeamId()); return new RankingItem(
                r.getRankNo(), r.getTeamId(), t == null ? null : t.getTeamName(), t == null ? null : t.getLogoUrl(),
                r.getTeamId(), t == null ? null : t.getTeamName(), String.valueOf(nvl(r.getPoints()))); }).toList();
        return card("STANDING", "POINTS", l, s, l.getLeagueName() + "积分榜", items);
    }
    private FeedCardVO playerCard(FootballLeague l, FootballSeason s, List<FootballPlayerCompetitionStat> rows,
                                  Map<Long, FootballPlayer> players, Map<Long, FootballTeam> teams) {
        List<RankingItem> items = java.util.stream.IntStream.range(0, rows.size()).mapToObj(i -> { var r = rows.get(i); var p = players.get(r.getPlayerId()); var t = teams.get(r.getTeamId()); return new RankingItem(
                i + 1, r.getPlayerId(), p == null ? null : p.getPlayerName(), p == null ? null : p.getAvatarUrl(),
                r.getTeamId(), t == null ? null : t.getTeamName(), String.valueOf(nvl(r.getGoals()))); }).toList();
        return card("PLAYER", "GOALS", l, s, l.getLeagueName() + "射手榜", items);
    }
    private FeedCardVO teamCard(FootballLeague l, FootballSeason s, List<FootballTeamCompetitionStat> rows, Map<Long, FootballTeam> teams) {
        List<RankingItem> items = java.util.stream.IntStream.range(0, rows.size()).mapToObj(i -> { var r = rows.get(i); var t = teams.get(r.getTeamId()); return new RankingItem(
                i + 1, r.getTeamId(), t == null ? null : t.getTeamName(), t == null ? null : t.getLogoUrl(),
                r.getTeamId(), t == null ? null : t.getTeamName(), String.valueOf(nvl(r.getGoalsFor()))); }).toList();
        return card("TEAM", "GOALS_FOR", l, s, l.getLeagueName() + "球队进球榜", items);
    }
    private FeedCardVO card(String type, String rankType, FootballLeague league, FootballSeason season,
                            String title, List<RankingItem> items) {
        String key = "RANKING:" + type + ":" + rankType + ":" + league.getId() + ":" + season.getId();
        RankingCardPayload payload = new RankingCardPayload(type, rankType, league.getId(), league.getLeagueName(),
                season.getId(), season.getSeasonName(), title, items);
        FeedCardVO card = new FeedCardVO(); card.setCardId(key.replace(':', '_')); card.setCardKey(key);
        card.setCardType("RANKING"); card.setAlgorithmVersion("AUX_RULE_V1"); card.setLeagueId(league.getId());
        card.setLeagueName(league.getLeagueName()); card.setTitle(title); card.setReasonCode("RANKING");
        card.setReason("你可能关注的榜单"); card.setPayload(payload); card.setScore(100D); return card;
    }
    private int nvl(Integer value) { return value == null ? 0 : value; }
    private <T> List<T> safe(List<T> rows) { return rows == null ? List.of() : rows; }
}
