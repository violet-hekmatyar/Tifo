package com.southstand.football.schedule.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.southstand.auth.security.CurrentUserHolder;
import com.southstand.auth.security.LoginUserContext;
import com.southstand.common.enums.ErrorCode;
import com.southstand.common.exception.BusinessException;
import com.southstand.common.result.PageResult;
import com.southstand.content.entity.Content;
import com.southstand.content.mapper.ContentMapper;
import com.southstand.follow.entity.FollowRecord;
import com.southstand.follow.mapper.FollowRecordMapper;
import com.southstand.football.event.entity.MatchEvent;
import com.southstand.football.event.mapper.MatchEventMapper;
import com.southstand.football.event.vo.MatchEventVO;
import com.southstand.football.league.entity.FootballLeague;
import com.southstand.football.league.mapper.FootballLeagueMapper;
import com.southstand.football.league.vo.LeagueVO;
import com.southstand.football.match.entity.MatchInfo;
import com.southstand.football.match.mapper.MatchInfoMapper;
import com.southstand.football.match.vo.MatchDetailVO;
import com.southstand.football.match.vo.MatchListVO;
import com.southstand.football.match.vo.MatchTeamVO;
import com.southstand.football.player.entity.FootballPlayer;
import com.southstand.football.player.entity.TeamPlayer;
import com.southstand.football.player.mapper.FootballPlayerMapper;
import com.southstand.football.player.mapper.TeamPlayerMapper;
import com.southstand.football.player.vo.PlayerDetailVO;
import com.southstand.football.player.vo.PlayerTeamVO;
import com.southstand.football.report.entity.MatchReport;
import com.southstand.football.report.mapper.MatchReportMapper;
import com.southstand.football.report.vo.MatchReportVO;
import com.southstand.football.team.entity.FootballTeam;
import com.southstand.football.team.mapper.FootballTeamMapper;
import com.southstand.football.team.vo.TeamDetailVO;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class FootballQueryService {

    private static final String ACTIVE = "ACTIVE";
    private static final String TEAM = "TEAM";
    private static final String PLAYER = "PLAYER";
    private static final int NOT_DELETED = 0;

    private final FootballLeagueMapper leagueMapper;
    private final FootballTeamMapper teamMapper;
    private final FootballPlayerMapper playerMapper;
    private final TeamPlayerMapper teamPlayerMapper;
    private final MatchInfoMapper matchInfoMapper;
    private final MatchEventMapper matchEventMapper;
    private final MatchReportMapper matchReportMapper;
    private final ContentMapper contentMapper;
    private final FollowRecordMapper followRecordMapper;

    public FootballQueryService(
            FootballLeagueMapper leagueMapper,
            FootballTeamMapper teamMapper,
            FootballPlayerMapper playerMapper,
            TeamPlayerMapper teamPlayerMapper,
            MatchInfoMapper matchInfoMapper,
            MatchEventMapper matchEventMapper,
            MatchReportMapper matchReportMapper,
            ContentMapper contentMapper,
            FollowRecordMapper followRecordMapper
    ) {
        this.leagueMapper = leagueMapper;
        this.teamMapper = teamMapper;
        this.playerMapper = playerMapper;
        this.teamPlayerMapper = teamPlayerMapper;
        this.matchInfoMapper = matchInfoMapper;
        this.matchEventMapper = matchEventMapper;
        this.matchReportMapper = matchReportMapper;
        this.contentMapper = contentMapper;
        this.followRecordMapper = followRecordMapper;
    }

    public List<LeagueVO> leagues() {
        List<FootballLeague> leagues = leagueMapper.selectList(activeLeagueWrapper());
        return leagues.stream().map(this::toLeagueVO).collect(Collectors.toList());
    }

    public PageResult<MatchListVO> importantMatches(LocalDate date, long pageNum, long pageSize) {
        QueryWrapper<MatchInfo> wrapper = activeMatchWrapper()
                .gt("important_level", 0)
                .orderByDesc("important_level")
                .orderByAsc("match_time");
        applyDate(wrapper, date);
        return page(matchInfoMapper.selectList(wrapper), pageNum, pageSize);
    }

    public PageResult<MatchListVO> matches(Long leagueId, Long teamId, LocalDate date, String status, long pageNum, long pageSize) {
        QueryWrapper<MatchInfo> wrapper = activeMatchWrapper().orderByAsc("match_time");
        if (leagueId != null) {
            wrapper.eq("league_id", leagueId);
        }
        if (teamId != null) {
            wrapper.and(w -> w.eq("home_team_id", teamId).or().eq("away_team_id", teamId));
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq("match_status", status);
        }
        applyDate(wrapper, date);
        return page(matchInfoMapper.selectList(wrapper), pageNum, pageSize);
    }

    public PageResult<MatchListVO> followingTeamMatches(Long teamId, long pageNum, long pageSize) {
        Long userId = CurrentUserHolder.get().getUserId();
        Set<Long> teamIds = followedTeamIds(userId);
        if (teamId != null) {
            if (!teamIds.contains(teamId)) {
                throw new BusinessException(ErrorCode.FORBIDDEN, "team is not followed");
            }
            teamIds = Set.of(teamId);
        }
        if (teamIds.isEmpty()) {
            return PageResult.of(Collections.emptyList(), 0, normalizePageNum(pageNum), normalizePageSize(pageSize));
        }
        Set<Long> queryTeamIds = teamIds;
        QueryWrapper<MatchInfo> wrapper = activeMatchWrapper()
                .and(w -> w.in("home_team_id", queryTeamIds).or().in("away_team_id", queryTeamIds))
                .orderByAsc("match_time");
        return page(matchInfoMapper.selectList(wrapper), pageNum, pageSize);
    }

    public TeamDetailVO teamDetail(Long teamId) {
        FootballTeam team = requireTeam(teamId);
        TeamDetailVO vo = new TeamDetailVO();
        vo.setTeamId(team.getId());
        vo.setTeamName(team.getTeamName());
        vo.setTeamNameEn(team.getTeamNameEn());
        vo.setShortName(team.getShortName());
        vo.setLogoUrl(team.getLogoUrl());
        vo.setCountry(team.getCountry());
        vo.setCity(team.getCity());
        vo.setStadiumName(team.getHomeStadium());
        vo.setFoundedYear(team.getFoundedYear());
        vo.setCoachName(team.getCoachName());
        vo.setMarketValue(team.getMarketValue());
        vo.setFollowerCount(team.getFollowerCount());
        vo.setFollowed(isFollowed(optionalUserId(), TEAM, teamId));
        vo.setRecentMatches(matchListForTeam(teamId, List.of("FINISHED"), false, 3));
        vo.setUpcomingMatches(matchListForTeam(teamId, List.of("SCHEDULED", "LIVE"), true, 3));
        return vo;
    }

    public PlayerDetailVO playerDetail(Long playerId) {
        FootballPlayer player = requirePlayer(playerId);
        TeamPlayer relation = teamPlayerMapper.selectOne(new QueryWrapper<TeamPlayer>()
                .eq("player_id", playerId)
                .eq("status", ACTIVE)
                .eq("is_deleted", NOT_DELETED)
                .orderByDesc("season")
                .last("LIMIT 1"));
        PlayerDetailVO vo = new PlayerDetailVO();
        vo.setPlayerId(player.getId());
        vo.setPlayerName(player.getPlayerName());
        vo.setPlayerNameEn(player.getPlayerNameEn());
        vo.setAvatarUrl(player.getAvatarUrl());
        vo.setPosition(player.getPosition());
        vo.setNationality(player.getNationality());
        vo.setAge(player.getBirthDate() == null ? null : Period.between(player.getBirthDate(), LocalDate.now()).getYears());
        vo.setShirtNumber(relation == null || relation.getShirtNumber() == null ? player.getShirtNumber() : relation.getShirtNumber());
        vo.setRetired(Objects.equals(player.getRetired(), 1));
        vo.setFollowerCount(player.getFollowerCount());
        vo.setTeam(toPlayerTeamVO(relation));
        vo.setFollowed(isFollowed(optionalUserId(), PLAYER, playerId));
        return vo;
    }

    public MatchDetailVO matchDetail(Long matchId) {
        MatchInfo match = requireMatch(matchId);
        MatchDetailVO vo = new MatchDetailVO();
        vo.setMatchId(match.getId());
        vo.setLeagueId(match.getLeagueId());
        vo.setLeagueName(leagueName(match.getLeagueId()));
        vo.setSeason(match.getSeason());
        vo.setRoundName(match.getRoundName());
        vo.setVenue(match.getVenue());
        vo.setHomeTeam(toMatchTeamVO(teamMapper.selectById(match.getHomeTeamId()), match.getHomeScore()));
        vo.setAwayTeam(toMatchTeamVO(teamMapper.selectById(match.getAwayTeamId()), match.getAwayScore()));
        vo.setMatchStatus(match.getMatchStatus());
        vo.setMatchTime(match.getMatchTime());
        vo.setEventList(eventList(matchId));
        vo.setReport(report(matchId));
        return vo;
    }

    private PageResult<MatchListVO> page(List<MatchInfo> matches, long pageNum, long pageSize) {
        long normalizedPageNum = normalizePageNum(pageNum);
        long normalizedPageSize = normalizePageSize(pageSize);
        int from = (int) Math.min((normalizedPageNum - 1) * normalizedPageSize, matches.size());
        int to = (int) Math.min(from + normalizedPageSize, matches.size());
        List<MatchListVO> records = matches.subList(from, to).stream().map(this::toMatchListVO).collect(Collectors.toList());
        return PageResult.of(records, matches.size(), normalizedPageNum, normalizedPageSize);
    }

    private List<MatchListVO> matchListForTeam(Long teamId, List<String> statuses, boolean ascending, int limit) {
        QueryWrapper<MatchInfo> wrapper = activeMatchWrapper()
                .and(w -> w.eq("home_team_id", teamId).or().eq("away_team_id", teamId))
                .in("match_status", statuses);
        if (ascending) {
            wrapper.orderByAsc("match_time");
        } else {
            wrapper.orderByDesc("match_time");
        }
        wrapper.last("LIMIT " + limit);
        return matchInfoMapper.selectList(wrapper).stream().map(this::toMatchListVO).collect(Collectors.toList());
    }

    private MatchListVO toMatchListVO(MatchInfo match) {
        MatchListVO vo = new MatchListVO();
        vo.setMatchId(match.getId());
        vo.setLeagueId(match.getLeagueId());
        vo.setLeagueName(leagueName(match.getLeagueId()));
        vo.setHomeTeam(toMatchTeamVO(teamMapper.selectById(match.getHomeTeamId()), match.getHomeScore()));
        vo.setAwayTeam(toMatchTeamVO(teamMapper.selectById(match.getAwayTeamId()), match.getAwayScore()));
        vo.setMatchStatus(match.getMatchStatus());
        vo.setMatchTime(match.getMatchTime());
        vo.setEventSummary(eventSummary(match.getId()));
        MatchReport report = firstReport(match.getId());
        vo.setHasReport(report != null || Objects.equals(match.getHasReport(), 1));
        vo.setReportContentId(report == null ? null : report.getContentId());
        return vo;
    }

    private MatchTeamVO toMatchTeamVO(FootballTeam team, Integer score) {
        MatchTeamVO vo = new MatchTeamVO();
        if (team != null) {
            vo.setTeamId(team.getId());
            vo.setTeamName(team.getTeamName());
            vo.setLogoUrl(team.getLogoUrl());
        }
        vo.setScore(score);
        return vo;
    }

    private LeagueVO toLeagueVO(FootballLeague league) {
        LeagueVO vo = new LeagueVO();
        vo.setLeagueId(league.getId());
        vo.setLeagueName(league.getLeagueName());
        vo.setLeagueNameEn(league.getLeagueNameEn());
        vo.setCountry(league.getCountry());
        vo.setLogoUrl(league.getLogoUrl());
        vo.setSeason(league.getSeason());
        vo.setLeagueType(league.getLeagueType());
        return vo;
    }

    private PlayerTeamVO toPlayerTeamVO(TeamPlayer relation) {
        if (relation == null) {
            return null;
        }
        FootballTeam team = teamMapper.selectById(relation.getTeamId());
        if (team == null) {
            return null;
        }
        PlayerTeamVO vo = new PlayerTeamVO();
        vo.setTeamId(team.getId());
        vo.setTeamName(team.getTeamName());
        vo.setLogoUrl(team.getLogoUrl());
        vo.setShirtNumber(relation.getShirtNumber());
        vo.setPosition(relation.getPosition());
        return vo;
    }

    private List<MatchEventVO> eventList(Long matchId) {
        List<MatchEvent> events = matchEventMapper.selectList(new QueryWrapper<MatchEvent>()
                .eq("match_id", matchId)
                .eq("status", ACTIVE)
                .eq("is_deleted", NOT_DELETED)
                .orderByAsc("minute")
                .orderByAsc("extra_minute")
                .orderByAsc("id"));
        Set<Long> teamIds = events.stream().map(MatchEvent::getTeamId).filter(Objects::nonNull).collect(Collectors.toCollection(LinkedHashSet::new));
        Set<Long> playerIds = new LinkedHashSet<>();
        events.stream().map(MatchEvent::getPlayerId).filter(Objects::nonNull).forEach(playerIds::add);
        events.stream().map(MatchEvent::getAssistPlayerId).filter(Objects::nonNull).forEach(playerIds::add);
        Map<Long, FootballTeam> teams = teamIds.isEmpty() ? Collections.emptyMap() : teamMapper.selectBatchIds(teamIds).stream()
                .collect(Collectors.toMap(FootballTeam::getId, Function.identity()));
        Map<Long, FootballPlayer> players = playerIds.isEmpty() ? Collections.emptyMap() : playerMapper.selectBatchIds(playerIds).stream()
                .collect(Collectors.toMap(FootballPlayer::getId, Function.identity()));
        List<MatchEventVO> result = new ArrayList<>();
        for (MatchEvent event : events) {
            MatchEventVO vo = new MatchEventVO();
            vo.setEventId(event.getId());
            vo.setEventType(event.getEventType());
            vo.setMinute(event.getMinute());
            vo.setExtraMinute(event.getExtraMinute());
            vo.setTeamId(event.getTeamId());
            vo.setTeamName(teamName(teams.get(event.getTeamId())));
            vo.setPlayerId(event.getPlayerId());
            vo.setPlayerName(playerName(players.get(event.getPlayerId())));
            vo.setAssistPlayerId(event.getAssistPlayerId());
            vo.setAssistPlayerName(playerName(players.get(event.getAssistPlayerId())));
            vo.setScoreAfter(event.getScoreAfter());
            vo.setDescription(event.getDescription());
            vo.setHasDebate(Objects.equals(event.getHasDebate(), 1));
            result.add(vo);
        }
        return result;
    }

    private MatchReportVO report(Long matchId) {
        MatchReport report = firstReport(matchId);
        if (report == null) {
            return null;
        }
        Content content = contentMapper.selectById(report.getContentId());
        MatchReportVO vo = new MatchReportVO();
        vo.setContentId(report.getContentId());
        vo.setReportType(report.getReportType());
        vo.setTitle(content == null ? null : content.getTitle());
        return vo;
    }

    private MatchReport firstReport(Long matchId) {
        return matchReportMapper.selectOne(new QueryWrapper<MatchReport>()
                .eq("match_id", matchId)
                .eq("status", ACTIVE)
                .eq("is_deleted", NOT_DELETED)
                .orderByAsc("id")
                .last("LIMIT 1"));
    }

    private String eventSummary(Long matchId) {
        MatchEvent event = matchEventMapper.selectOne(new QueryWrapper<MatchEvent>()
                .eq("match_id", matchId)
                .eq("status", ACTIVE)
                .eq("is_deleted", NOT_DELETED)
                .orderByDesc("minute")
                .orderByDesc("id")
                .last("LIMIT 1"));
        if (event == null) {
            return null;
        }
        FootballPlayer player = event.getPlayerId() == null ? null : playerMapper.selectById(event.getPlayerId());
        String minute = event.getMinute() == null ? "" : event.getMinute() + "'";
        String name = player == null ? "" : " " + player.getPlayerName();
        return (minute + " " + event.getEventType() + name).trim();
    }

    private FootballTeam requireTeam(Long teamId) {
        FootballTeam team = teamMapper.selectById(teamId);
        if (team == null || !ACTIVE.equals(team.getStatus()) || !Objects.equals(team.getIsDeleted(), NOT_DELETED)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "team not found");
        }
        return team;
    }

    private FootballPlayer requirePlayer(Long playerId) {
        FootballPlayer player = playerMapper.selectById(playerId);
        if (player == null || !ACTIVE.equals(player.getStatus()) || !Objects.equals(player.getIsDeleted(), NOT_DELETED)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "player not found");
        }
        return player;
    }

    private MatchInfo requireMatch(Long matchId) {
        MatchInfo match = matchInfoMapper.selectById(matchId);
        if (match == null || !ACTIVE.equals(match.getStatus()) || !Objects.equals(match.getIsDeleted(), NOT_DELETED)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "match not found");
        }
        return match;
    }

    private QueryWrapper<FootballLeague> activeLeagueWrapper() {
        return new QueryWrapper<FootballLeague>()
                .eq("status", ACTIVE)
                .eq("is_deleted", NOT_DELETED)
                .orderByAsc("sort_order")
                .orderByAsc("id");
    }

    private QueryWrapper<MatchInfo> activeMatchWrapper() {
        return new QueryWrapper<MatchInfo>()
                .eq("status", ACTIVE)
                .eq("is_deleted", NOT_DELETED);
    }

    private void applyDate(QueryWrapper<MatchInfo> wrapper, LocalDate date) {
        if (date != null) {
            wrapper.ge("match_time", date.atStartOfDay());
            wrapper.lt("match_time", date.plusDays(1).atStartOfDay());
        }
    }

    private Set<Long> followedTeamIds(Long userId) {
        return followRecordMapper.selectList(new QueryWrapper<FollowRecord>()
                        .eq("user_id", userId)
                        .eq("follow_type", TEAM)
                        .eq("status", ACTIVE)
                        .eq("is_deleted", NOT_DELETED))
                .stream()
                .map(FollowRecord::getTargetId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private boolean isFollowed(Long userId, String type, Long targetId) {
        if (userId == null || targetId == null) {
            return false;
        }
        Long count = followRecordMapper.selectCount(new QueryWrapper<FollowRecord>()
                .eq("user_id", userId)
                .eq("follow_type", type)
                .eq("target_id", targetId)
                .eq("status", ACTIVE)
                .eq("is_deleted", NOT_DELETED));
        return count != null && count > 0;
    }

    private Long optionalUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof LoginUserContext context)) {
            return null;
        }
        return context.getUserId();
    }

    private String leagueName(Long leagueId) {
        FootballLeague league = leagueId == null ? null : leagueMapper.selectById(leagueId);
        return league == null ? null : league.getLeagueName();
    }

    private String teamName(FootballTeam team) {
        return team == null ? null : team.getTeamName();
    }

    private String playerName(FootballPlayer player) {
        return player == null ? null : player.getPlayerName();
    }

    private long normalizePageNum(long pageNum) {
        return pageNum <= 0 ? 1 : pageNum;
    }

    private long normalizePageSize(long pageSize) {
        if (pageSize <= 0) {
            return 10;
        }
        return Math.min(pageSize, 50);
    }
}
