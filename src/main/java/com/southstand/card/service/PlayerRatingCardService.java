package com.southstand.card.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.southstand.card.model.HomeFeedUserContext;
import com.southstand.card.vo.FeedCardVO;
import com.southstand.card.vo.PlayerRatingCardPayload;
import com.southstand.card.vo.PlayerRatingCardPayload.RatingPlayer;
import com.southstand.football.league.entity.FootballLeague;
import com.southstand.football.league.mapper.FootballLeagueMapper;
import com.southstand.football.match.entity.MatchInfo;
import com.southstand.football.match.mapper.MatchInfoMapper;
import com.southstand.football.match.vo.MatchTeamVO;
import com.southstand.football.matchdata.entity.FootballMatchPlayerStat;
import com.southstand.football.matchdata.entity.FootballUserPlayerRating;
import com.southstand.football.matchdata.mapper.FootballMatchPlayerStatMapper;
import com.southstand.football.matchdata.mapper.FootballUserPlayerRatingMapper;
import com.southstand.football.player.entity.FootballPlayer;
import com.southstand.football.player.mapper.FootballPlayerMapper;
import com.southstand.football.team.entity.FootballTeam;
import com.southstand.football.team.mapper.FootballTeamMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
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
public class PlayerRatingCardService {
    private static final String ACTIVE = "ACTIVE";
    private static final int NOT_DELETED = 0;
    private final MatchInfoMapper matchMapper;
    private final FootballMatchPlayerStatMapper statMapper;
    private final FootballUserPlayerRatingMapper ratingMapper;
    private final FootballLeagueMapper leagueMapper;
    private final FootballTeamMapper teamMapper;
    private final FootballPlayerMapper playerMapper;

    public PlayerRatingCardService(MatchInfoMapper matchMapper, FootballMatchPlayerStatMapper statMapper,
                                   FootballUserPlayerRatingMapper ratingMapper, FootballLeagueMapper leagueMapper,
                                   FootballTeamMapper teamMapper, FootballPlayerMapper playerMapper) {
        this.matchMapper = matchMapper; this.statMapper = statMapper; this.ratingMapper = ratingMapper;
        this.leagueMapper = leagueMapper; this.teamMapper = teamMapper; this.playerMapper = playerMapper;
    }

    public List<FeedCardVO> candidates(HomeFeedUserContext user) {
        List<MatchInfo> matches = safe(matchMapper.selectList(new QueryWrapper<MatchInfo>()
                .eq("match_status", "FINISHED").eq("status", ACTIVE).eq("is_deleted", NOT_DELETED)
                .orderByDesc("match_time").orderByDesc("important_level").orderByAsc("id").last("LIMIT 30")));
        if (matches.isEmpty()) return List.of();
        matches = matches.stream().sorted(Comparator.comparingInt((MatchInfo m) -> priority(m, user)).reversed()
                .thenComparing(MatchInfo::getImportantLevel, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(MatchInfo::getMatchTime, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(MatchInfo::getId)).toList();
        Set<Long> matchIds = matches.stream().map(MatchInfo::getId).collect(Collectors.toCollection(LinkedHashSet::new));
        List<FootballMatchPlayerStat> stats = safe(statMapper.selectList(new QueryWrapper<FootballMatchPlayerStat>()
                .in("match_id", matchIds).eq("is_deleted", NOT_DELETED)));
        if (stats.isEmpty()) return List.of();
        Map<Long, List<FootballMatchPlayerStat>> statsByMatch = stats.stream().collect(Collectors.groupingBy(
                FootballMatchPlayerStat::getMatchId, LinkedHashMap::new, Collectors.toList()));
        MatchInfo selected = matches.stream().filter(m -> !statsByMatch.getOrDefault(m.getId(), List.of()).isEmpty())
                .findFirst().orElse(null);
        if (selected == null) return List.of();
        List<FootballMatchPlayerStat> selectedStats = statsByMatch.get(selected.getId());
        List<FootballUserPlayerRating> ratings = safe(ratingMapper.selectList(new QueryWrapper<FootballUserPlayerRating>()
                .eq("match_id", selected.getId()).eq("status", ACTIVE).eq("is_deleted", NOT_DELETED)));
        Map<Long, RatingAggregate> aggregates = ratings.stream().collect(Collectors.groupingBy(
                FootballUserPlayerRating::getPlayerId, LinkedHashMap::new,
                Collectors.collectingAndThen(Collectors.toList(), this::aggregate)));
        Set<Long> playerIds = selectedStats.stream().map(FootballMatchPlayerStat::getPlayerId).collect(Collectors.toSet());
        Map<Long, FootballPlayer> players = playerIds.isEmpty() ? Map.of() : safe(playerMapper.selectBatchIds(playerIds)).stream()
                .collect(Collectors.toMap(FootballPlayer::getId, Function.identity(), (a, b) -> a, LinkedHashMap::new));
        Set<Long> teamIds = new LinkedHashSet<>();
        if (selected.getHomeTeamId() != null) teamIds.add(selected.getHomeTeamId());
        if (selected.getAwayTeamId() != null) teamIds.add(selected.getAwayTeamId());
        Map<Long, FootballTeam> teams = safe(teamMapper.selectBatchIds(teamIds)).stream().collect(Collectors.toMap(
                FootballTeam::getId, Function.identity(), (a, b) -> a, LinkedHashMap::new));
        FootballLeague league = leagueMapper.selectById(selected.getLeagueId());
        List<RatingPlayer> topPlayers = selectedStats.stream().filter(s -> players.containsKey(s.getPlayerId()))
                .sorted(Comparator.comparingInt((FootballMatchPlayerStat s) -> aggregates.getOrDefault(s.getPlayerId(), RatingAggregate.EMPTY).count()).reversed()
                        .thenComparing((FootballMatchPlayerStat s) -> displayScore(s, aggregates.get(s.getPlayerId())), Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(FootballMatchPlayerStat::getPlayerId))
                .limit(5).map(s -> { FootballPlayer p = players.get(s.getPlayerId()); RatingAggregate a = aggregates.get(s.getPlayerId());
                    return new RatingPlayer(s.getPlayerId(), p.getPlayerName(), p.getAvatarUrl(), s.getTeamId(),
                            s.getOfficialRating(), a == null ? null : a.average(), a == null ? 0 : a.count()); }).toList();
        if (topPlayers.isEmpty()) return List.of();
        int userCount = (int) ratings.stream().map(FootballUserPlayerRating::getUserId).distinct().count();
        String reasonCode = priority(selected, user) >= 3 ? "MAIN_TEAM" : priority(selected, user) == 2 ? "FOLLOWED_TEAM" : "POST_MATCH_RATING";
        return List.of(card(selected, league, teams, topPlayers, userCount, reasonCode));
    }

    private FeedCardVO card(MatchInfo match, FootballLeague league, Map<Long, FootballTeam> teams,
                            List<RatingPlayer> players, int userCount, String reasonCode) {
        MatchTeamVO home = team(teams.get(match.getHomeTeamId()), match.getHomeScore());
        MatchTeamVO away = team(teams.get(match.getAwayTeamId()), match.getAwayScore());
        PlayerRatingCardPayload payload = new PlayerRatingCardPayload(match.getId(), match.getLeagueId(),
                league == null ? null : league.getLeagueName(), match.getMatchTime(), home, away,
                match.getHomeScore(), match.getAwayScore(), players, userCount);
        FeedCardVO card = new FeedCardVO(); card.setCardId("PLAYER_RATING_" + match.getId());
        card.setCardKey("PLAYER_RATING:" + match.getId()); card.setCardType("PLAYER_RATING");
        card.setAlgorithmVersion("AUX_RULE_V1"); card.setMatchId(match.getId()); card.setLeagueId(match.getLeagueId());
        card.setLeagueName(league == null ? null : league.getLeagueName()); card.setHomeTeam(home); card.setAwayTeam(away);
        card.setHomeScore(match.getHomeScore()); card.setAwayScore(match.getAwayScore()); card.setMatchTime(match.getMatchTime());
        card.setTitle((home.getTeamName() == null ? "主队" : home.getTeamName()) + " vs " + (away.getTeamName() == null ? "客队" : away.getTeamName()) + " 赛后评分");
        card.setReasonCode(reasonCode); card.setReason(switch (reasonCode) { case "MAIN_TEAM" -> "主队赛后评分"; case "FOLLOWED_TEAM" -> "关注球队赛后评分"; default -> "赛后球员评分"; });
        card.setPayload(payload); card.setScore(120D + (match.getImportantLevel() == null ? 0 : match.getImportantLevel() * 5D)); return card;
    }

    private int priority(MatchInfo m, HomeFeedUserContext user) {
        if (user.mainTeamId() != null && (Objects.equals(m.getHomeTeamId(), user.mainTeamId()) || Objects.equals(m.getAwayTeamId(), user.mainTeamId()))) return 3;
        if (user.followedTeamIds().contains(m.getHomeTeamId()) || user.followedTeamIds().contains(m.getAwayTeamId())) return 2;
        return 1;
    }
    private BigDecimal displayScore(FootballMatchPlayerStat s, RatingAggregate a) { return a != null && a.count() > 0 ? a.average() : s.getOfficialRating(); }
    private RatingAggregate aggregate(List<FootballUserPlayerRating> rows) { if (rows.isEmpty()) return RatingAggregate.EMPTY; BigDecimal sum = rows.stream().map(FootballUserPlayerRating::getRating).reduce(BigDecimal.ZERO, BigDecimal::add); return new RatingAggregate(sum.divide(BigDecimal.valueOf(rows.size()), 2, RoundingMode.HALF_UP), rows.size()); }
    private MatchTeamVO team(FootballTeam t, Integer score) { MatchTeamVO vo = new MatchTeamVO(); if (t != null) { vo.setTeamId(t.getId()); vo.setTeamName(t.getTeamName()); vo.setLogoUrl(t.getLogoUrl()); } vo.setScore(score); return vo; }
    private <T> List<T> safe(List<T> rows) { return rows == null ? List.of() : rows; }
    private record RatingAggregate(BigDecimal average, int count) { private static final RatingAggregate EMPTY = new RatingAggregate(null, 0); }
}
