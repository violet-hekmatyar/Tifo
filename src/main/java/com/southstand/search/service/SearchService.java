package com.southstand.search.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.southstand.common.enums.ErrorCode;
import com.southstand.common.exception.BusinessException;
import com.southstand.common.result.PageResult;
import com.southstand.football.league.entity.FootballLeague;
import com.southstand.football.league.mapper.FootballLeagueMapper;
import com.southstand.football.match.entity.MatchInfo;
import com.southstand.football.match.mapper.MatchInfoMapper;
import com.southstand.football.player.entity.FootballPlayer;
import com.southstand.football.player.entity.TeamPlayer;
import com.southstand.football.player.mapper.FootballPlayerMapper;
import com.southstand.football.player.mapper.TeamPlayerMapper;
import com.southstand.football.team.entity.FootballTeam;
import com.southstand.football.team.mapper.FootballTeamMapper;
import com.southstand.search.vo.SearchEntityVO;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class SearchService {

    private static final String ACTIVE = "ACTIVE";
    private static final int NOT_DELETED = 0;

    private final FootballTeamMapper teamMapper;
    private final FootballPlayerMapper playerMapper;
    private final TeamPlayerMapper teamPlayerMapper;
    private final MatchInfoMapper matchMapper;
    private final FootballLeagueMapper leagueMapper;

    public SearchService(
            FootballTeamMapper teamMapper,
            FootballPlayerMapper playerMapper,
            TeamPlayerMapper teamPlayerMapper,
            MatchInfoMapper matchMapper,
            FootballLeagueMapper leagueMapper
    ) {
        this.teamMapper = teamMapper;
        this.playerMapper = playerMapper;
        this.teamPlayerMapper = teamPlayerMapper;
        this.matchMapper = matchMapper;
        this.leagueMapper = leagueMapper;
    }

    public PageResult<SearchEntityVO> entities(String keyword, String entityType, Long pageNum, Long pageSize) {
        String kw = keyword == null ? "" : keyword.trim();
        if (kw.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "keyword required");
        }
        String type = entityType == null || entityType.trim().isEmpty() ? null : entityType.trim().toUpperCase();
        long safePageNum = pageNum == null || pageNum < 1 ? 1 : pageNum;
        long safePageSize = pageSize == null || pageSize < 1 ? 20 : Math.min(pageSize, 50);
        List<SearchEntityVO> all = new ArrayList<>();
        if (type == null || "TEAM".equals(type)) {
            all.addAll(searchTeams(kw));
        }
        if (type == null || "PLAYER".equals(type)) {
            all.addAll(searchPlayers(kw));
        }
        if (type == null || "MATCH".equals(type)) {
            all.addAll(searchMatches(kw));
        }
        if (type != null && !"TEAM".equals(type) && !"PLAYER".equals(type) && !"MATCH".equals(type)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "unsupported entityType");
        }
        all.sort(Comparator.comparingInt((SearchEntityVO vo) -> typeOrder(vo.getEntityType()))
                .thenComparing(SearchEntityVO::getName, Comparator.nullsLast(String::compareToIgnoreCase))
                .thenComparing(SearchEntityVO::getEntityId));
        int from = (int) Math.min((safePageNum - 1) * safePageSize, all.size());
        int to = (int) Math.min(from + safePageSize, all.size());
        return PageResult.of(all.subList(from, to), all.size(), safePageNum, safePageSize);
    }

    private List<SearchEntityVO> searchTeams(String keyword) {
        String like = like(keyword);
        return teamMapper.selectList(new QueryWrapper<FootballTeam>()
                        .eq("status", ACTIVE)
                        .eq("is_deleted", NOT_DELETED)
                        .and(w -> w.like("team_name", like).or().like("team_name_en", like).or().like("short_name", like))
                        .orderByAsc("id")
                        .last("LIMIT 50"))
                .stream()
                .map(this::teamVO)
                .toList();
    }

    private List<SearchEntityVO> searchPlayers(String keyword) {
        String like = like(keyword);
        List<FootballPlayer> players = playerMapper.selectList(new QueryWrapper<FootballPlayer>()
                .eq("status", ACTIVE)
                .eq("is_deleted", NOT_DELETED)
                .and(w -> w.like("player_name", like).or().like("player_name_en", like))
                .orderByAsc("id")
                .last("LIMIT 50"));
        List<Long> playerIds = players.stream().map(FootballPlayer::getId).toList();
        Map<Long, Long> teamIdByPlayer = playerIds.isEmpty() ? Map.of() : teamPlayerMapper.selectList(new QueryWrapper<TeamPlayer>()
                        .in("player_id", playerIds)
                        .eq("status", ACTIVE)
                        .eq("is_deleted", NOT_DELETED)
                        .orderByAsc("id"))
                .stream()
                .collect(Collectors.toMap(TeamPlayer::getPlayerId, TeamPlayer::getTeamId, (a, b) -> a));
        return players.stream().map(player -> playerVO(player, teamIdByPlayer.get(player.getId()))).toList();
    }

    private List<SearchEntityVO> searchMatches(String keyword) {
        String like = like(keyword);
        List<FootballTeam> teams = teamMapper.selectList(new QueryWrapper<FootballTeam>()
                .eq("status", ACTIVE)
                .eq("is_deleted", NOT_DELETED)
                .and(w -> w.like("team_name", like).or().like("team_name_en", like).or().like("short_name", like)));
        List<FootballLeague> leagues = leagueMapper.selectList(new QueryWrapper<FootballLeague>()
                .eq("status", ACTIVE)
                .eq("is_deleted", NOT_DELETED)
                .and(w -> w.like("league_name", like).or().like("league_name_en", like)));
        List<Long> teamIds = teams.stream().map(FootballTeam::getId).toList();
        List<Long> leagueIds = leagues.stream().map(FootballLeague::getId).toList();
        QueryWrapper<MatchInfo> wrapper = new QueryWrapper<MatchInfo>()
                .eq("status", ACTIVE)
                .eq("is_deleted", NOT_DELETED);
        if (isLong(keyword)) {
            wrapper.and(w -> w.eq("id", Long.parseLong(keyword))
                    .or(!teamIds.isEmpty(), x -> x.in("home_team_id", teamIds).or().in("away_team_id", teamIds))
                    .or(!leagueIds.isEmpty(), x -> x.in("league_id", leagueIds)));
        } else if (!teamIds.isEmpty() || !leagueIds.isEmpty()) {
            wrapper.and(w -> {
                boolean hasAny = false;
                if (!teamIds.isEmpty()) {
                    w.in("home_team_id", teamIds).or().in("away_team_id", teamIds);
                    hasAny = true;
                }
                if (!leagueIds.isEmpty()) {
                    if (hasAny) {
                        w.or();
                    }
                    w.in("league_id", leagueIds);
                }
            });
        } else {
            return List.of();
        }
        return matchMapper.selectList(wrapper.orderByAsc("match_time").orderByAsc("id").last("LIMIT 50"))
                .stream()
                .map(this::matchVO)
                .toList();
    }

    private SearchEntityVO teamVO(FootballTeam team) {
        SearchEntityVO vo = new SearchEntityVO();
        vo.setEntityType("TEAM");
        vo.setEntityId(team.getId());
        vo.setName(team.getTeamName());
        vo.setNameEn(team.getTeamNameEn());
        vo.setSubtitle(team.getCountry());
        vo.setLogoUrl(team.getLogoUrl());
        vo.setStatus(team.getStatus());
        return vo;
    }

    private SearchEntityVO playerVO(FootballPlayer player, Long teamId) {
        FootballTeam team = teamId == null ? null : teamMapper.selectById(teamId);
        SearchEntityVO vo = new SearchEntityVO();
        vo.setEntityType("PLAYER");
        vo.setEntityId(player.getId());
        vo.setName(player.getPlayerName());
        vo.setNameEn(player.getPlayerNameEn());
        vo.setSubtitle((team == null ? "" : team.getTeamName() + " · ") + Objects.toString(player.getPosition(), ""));
        vo.setAvatarUrl(player.getAvatarUrl());
        vo.setStatus(player.getStatus());
        return vo;
    }

    private SearchEntityVO matchVO(MatchInfo match) {
        FootballTeam home = teamMapper.selectById(match.getHomeTeamId());
        FootballTeam away = teamMapper.selectById(match.getAwayTeamId());
        FootballLeague league = leagueMapper.selectById(match.getLeagueId());
        SearchEntityVO vo = new SearchEntityVO();
        vo.setEntityType("MATCH");
        vo.setEntityId(match.getId());
        vo.setName(teamName(home) + " vs " + teamName(away));
        vo.setSubtitle((league == null ? "" : league.getLeagueName() + " · ") + match.getMatchTime());
        vo.setHomeTeamId(match.getHomeTeamId());
        vo.setAwayTeamId(match.getAwayTeamId());
        vo.setMatchStatus(match.getMatchStatus());
        vo.setMatchTime(match.getMatchTime());
        vo.setStatus(match.getStatus());
        return vo;
    }

    private String teamName(FootballTeam team) {
        return team == null ? null : team.getTeamName();
    }

    private String like(String keyword) {
        return keyword.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    private boolean isLong(String value) {
        try {
            Long.parseLong(value);
            return true;
        } catch (NumberFormatException ex) {
            return false;
        }
    }

    private int typeOrder(String type) {
        return switch (type) {
            case "TEAM" -> 1;
            case "PLAYER" -> 2;
            case "MATCH" -> 3;
            default -> 9;
        };
    }
}
