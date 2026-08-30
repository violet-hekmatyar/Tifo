package com.southstand.football.detail.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.southstand.auth.security.LoginUserContext;
import com.southstand.common.enums.ErrorCode;
import com.southstand.common.exception.BusinessException;
import com.southstand.common.result.PageResult;
import com.southstand.content.entity.Content;
import com.southstand.content.entity.ContentRelation;
import com.southstand.content.mapper.ContentMapper;
import com.southstand.content.mapper.ContentRelationMapper;
import com.southstand.follow.entity.FollowRecord;
import com.southstand.follow.mapper.FollowRecordMapper;
import com.southstand.football.detail.entity.FootballPlayerTeamHistory;
import com.southstand.football.detail.entity.FootballTeamHonor;
import com.southstand.football.detail.entity.FootballTeamSeasonPlayer;
import com.southstand.football.detail.mapper.FootballPlayerTeamHistoryMapper;
import com.southstand.football.detail.mapper.FootballTeamHonorMapper;
import com.southstand.football.detail.mapper.FootballTeamSeasonPlayerMapper;
import com.southstand.football.detail.vo.FootballDetailVO.*;
import com.southstand.football.league.entity.FootballLeague;
import com.southstand.football.league.mapper.FootballLeagueMapper;
import com.southstand.football.match.entity.MatchInfo;
import com.southstand.football.match.mapper.MatchInfoMapper;
import com.southstand.football.matchdata.entity.FootballMatchPlayerAppearance;
import com.southstand.football.matchdata.entity.FootballMatchPlayerStat;
import com.southstand.football.matchdata.mapper.FootballMatchPlayerAppearanceMapper;
import com.southstand.football.matchdata.mapper.FootballMatchPlayerStatMapper;
import com.southstand.football.player.entity.FootballPlayer;
import com.southstand.football.player.entity.TeamPlayer;
import com.southstand.football.player.mapper.FootballPlayerMapper;
import com.southstand.football.player.mapper.TeamPlayerMapper;
import com.southstand.football.rank.entity.FootballPlayerCompetitionStat;
import com.southstand.football.rank.entity.FootballSeason;
import com.southstand.football.rank.entity.FootballStanding;
import com.southstand.football.rank.entity.FootballTeamCompetitionStat;
import com.southstand.football.rank.mapper.FootballPlayerCompetitionStatMapper;
import com.southstand.football.rank.mapper.FootballSeasonMapper;
import com.southstand.football.rank.mapper.FootballStandingMapper;
import com.southstand.football.rank.mapper.FootballTeamCompetitionStatMapper;
import com.southstand.football.team.entity.FootballTeam;
import com.southstand.football.team.mapper.FootballTeamMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class FootballDetailService {
    private static final String ACTIVE="ACTIVE";
    private final FootballTeamMapper teams; private final FootballPlayerMapper players;
    private final FootballLeagueMapper leagues; private final FootballSeasonMapper seasons;
    private final FootballTeamSeasonPlayerMapper rosters; private final FootballTeamHonorMapper honors;
    private final FootballPlayerTeamHistoryMapper histories; private final FootballPlayerCompetitionStatMapper playerStats;
    private final FootballTeamCompetitionStatMapper teamStats; private final FootballStandingMapper standings;
    private final MatchInfoMapper matches; private final ContentRelationMapper contentRelations; private final ContentMapper contents;
    private final FollowRecordMapper follows;
    private final TeamPlayerMapper teamPlayers;
    private final FootballMatchPlayerAppearanceMapper matchAppearances;
    private final FootballMatchPlayerStatMapper matchPlayerStats;

    @Autowired
    public FootballDetailService(FootballTeamMapper teams,FootballPlayerMapper players,FootballLeagueMapper leagues,
            FootballSeasonMapper seasons,FootballTeamSeasonPlayerMapper rosters,FootballTeamHonorMapper honors,
            FootballPlayerTeamHistoryMapper histories,FootballPlayerCompetitionStatMapper playerStats,
            FootballTeamCompetitionStatMapper teamStats,FootballStandingMapper standings,MatchInfoMapper matches,
            ContentRelationMapper contentRelations,ContentMapper contents,FollowRecordMapper follows,
            TeamPlayerMapper teamPlayers,FootballMatchPlayerAppearanceMapper matchAppearances,
            FootballMatchPlayerStatMapper matchPlayerStats){
        this.teams=teams;this.players=players;this.leagues=leagues;this.seasons=seasons;this.rosters=rosters;this.honors=honors;
        this.histories=histories;this.playerStats=playerStats;this.teamStats=teamStats;this.standings=standings;
        this.matches=matches;this.contentRelations=contentRelations;this.contents=contents;this.follows=follows;
        this.teamPlayers=teamPlayers;this.matchAppearances=matchAppearances;this.matchPlayerStats=matchPlayerStats;
    }

    public FootballDetailService(FootballTeamMapper teams,FootballPlayerMapper players,FootballLeagueMapper leagues,
            FootballSeasonMapper seasons,FootballTeamSeasonPlayerMapper rosters,FootballTeamHonorMapper honors,
            FootballPlayerTeamHistoryMapper histories,FootballPlayerCompetitionStatMapper playerStats,
            FootballTeamCompetitionStatMapper teamStats,FootballStandingMapper standings,MatchInfoMapper matches,
            ContentRelationMapper contentRelations,ContentMapper contents,FollowRecordMapper follows){
        this(teams,players,leagues,seasons,rosters,honors,histories,playerStats,teamStats,standings,matches,
                contentRelations,contents,follows,null,null,null);
    }

    public TeamOverview teamOverview(Long teamId,Long seasonId){
        FootballTeam team=requireTeam(teamId); Scope scope=resolveTeamScope(teamId,seasonId);
        FootballLeague league=leagues.selectById(scope.season.getLeagueId());
        TeamStats stats=teamStats(teamId,scope.season.getId(),null);
        Standing standing=standing(teamId,scope.season.getId(),null);
        List<RosterPlayer> squad=teamPlayers(teamId,scope.season.getId(),null,null,1,100).getRecords();
        List<RosterPlayer> scorers=squad.stream().sorted(Comparator.comparingInt((RosterPlayer x)->nz(x.goals())).reversed().thenComparing(RosterPlayer::playerId)).limit(5).toList();
        List<RosterPlayer> assists=squad.stream().sorted(Comparator.comparingInt((RosterPlayer x)->nz(x.assists())).reversed().thenComparing(RosterPlayer::playerId)).limit(5).toList();
        List<Match> recent=teamMatches(teamId,List.of("FINISHED"),false,5);
        List<Match> next=teamMatches(teamId,List.of("SCHEDULED","LIVE"),true,1);
        return new TeamOverview(team.getId(),team.getTeamName(),team.getTeamNameEn(),team.getLogoUrl(),scope.season.getLeagueId(),
                league==null?null:league.getLeagueName(),scope.season.getId(),scope.season.getSeasonName(),team.getCity(),team.getHomeStadium(),
                team.getFoundedYear(),team.getRemark(),isFollowed("TEAM",teamId),standing,stats,scorers,assists,recent,
                next.isEmpty()?null:next.get(0),recentContents("TEAM",teamId),competitionStandings(teamId),
                leaderboards(squad),teamHonors(teamId,null));
    }

    public PageResult<RosterPlayer> teamPlayers(Long teamId,Long seasonId,String position,String squadRole,long pageNum,long pageSize){
        requireTeam(teamId); Scope scope=resolveTeamScope(teamId,seasonId);
        QueryWrapper<FootballTeamSeasonPlayer> q=new QueryWrapper<FootballTeamSeasonPlayer>().eq("team_id",teamId).eq("season_id",scope.season.getId()).eq("status",ACTIVE).eq("is_deleted",0);
        if(StringUtils.hasText(position))q.eq("position",normalizePosition(position));
        if(StringUtils.hasText(squadRole))q.eq("squad_role",squadRole.toUpperCase());
        List<FootballTeamSeasonPlayer> rows=new ArrayList<>(rosters.selectList(q));
        rows.sort(Comparator.comparingInt((FootballTeamSeasonPlayer x)->positionOrder(x.getPosition())).thenComparing(x->x.getShirtNumber()==null?999:x.getShirtNumber()).thenComparing(FootballTeamSeasonPlayer::getPlayerId));
        Set<Long> ids=rows.stream().map(FootballTeamSeasonPlayer::getPlayerId).collect(Collectors.toCollection(LinkedHashSet::new));
        Map<Long,FootballPlayer> playerMap=ids.isEmpty()?Map.of():batch(players.selectBatchIds(ids),FootballPlayer::getId);
        List<FootballPlayerCompetitionStat> statRows=ids.isEmpty()?List.of():playerStats.selectList(new QueryWrapper<FootballPlayerCompetitionStat>().eq("season_id",scope.season.getId()).eq("team_id",teamId).in("player_id",ids).eq("is_deleted",0));
        Map<Long,FootballPlayerCompetitionStat> statMap=statRows.stream().collect(Collectors.toMap(FootballPlayerCompetitionStat::getPlayerId,Function.identity(),(a,b)->a));
        Set<Long> loanIds=rows.stream().map(FootballTeamSeasonPlayer::getLoanFromTeamId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long,FootballTeam> loanTeams=loanIds.isEmpty()?Map.of():batch(teams.selectBatchIds(loanIds),FootballTeam::getId);
        Set<Long> followed=followedIds("PLAYER",ids);
        List<RosterPlayer> result=rows.stream().map(r->rosterPlayer(r,playerMap.get(r.getPlayerId()),statMap.get(r.getPlayerId()),r.getLoanFromTeamId()==null?null:loanTeams.get(r.getLoanFromTeamId()),followed.contains(r.getPlayerId()))).toList();
        long pn=Math.max(1,pageNum),ps=Math.min(100,Math.max(1,pageSize)); int from=(int)Math.min((pn-1)*ps,result.size()),to=(int)Math.min(from+ps,result.size());
        return PageResult.of(result.subList(from,to),result.size(),pn,ps);
    }

    public TeamStats teamStats(Long teamId,Long seasonId,Long stageId){
        requireTeam(teamId); Scope scope=resolveTeamScope(teamId,seasonId);
        QueryWrapper<FootballTeamCompetitionStat> q=new QueryWrapper<FootballTeamCompetitionStat>().eq("team_id",teamId).eq("season_id",scope.season.getId()).eq("is_deleted",0);
        if(stageId!=null)q.eq("stage_id",stageId); else q.orderByAsc("stage_id").last("LIMIT 1");
        FootballTeamCompetitionStat s=teamStats.selectOne(q); FootballStanding st=standingEntity(teamId,scope.season.getId(),stageId);
        if(s==null)return new TeamStats(0,0,0,0,0,0,0,BigDecimal.ZERO,0,0,0,0,0,null,st==null?null:st.getRankNo(),st==null?0:st.getPoints(),null,null);
        return new TeamStats(nz(s.getPlayed()),nz(s.getGoalsFor()),nz(s.getGoalsAgainst()),nz(s.getGoalsFor())-nz(s.getGoalsAgainst()),nz(s.getAssists()),nz(s.getShots()),nz(s.getShotsOnTarget()),ratio(s.getShotsOnTarget(),s.getShots()),nz(s.getCorners()),nz(s.getFouls()),nz(s.getYellowCards()),nz(s.getRedCards()),nz(s.getCleanSheets()),s.getAvgRating(),st==null?null:st.getRankNo(),st==null?0:st.getPoints(),s.getSource(),s.getSourceUpdatedAt());
    }

    public List<Honor> teamHonors(Long teamId,String honorType){
        requireTeam(teamId); QueryWrapper<FootballTeamHonor> q=new QueryWrapper<FootballTeamHonor>().eq("team_id",teamId).eq("is_deleted",0);
        if(StringUtils.hasText(honorType))q.eq("honor_type",honorType.toUpperCase());
        q.orderByDesc("latest_year").orderByDesc("title_count").orderByAsc("id");
        return honors.selectList(q).stream().map(h->new Honor(h.getId(),h.getHonorName(),h.getHonorType(),h.getTitleCount(),years(h.getWinningYears()),h.getLatestYear())).toList();
    }

    public PlayerOverview playerOverview(Long playerId,Long seasonId){
        FootballPlayer p=requirePlayer(playerId); List<TeamHistory> teamHistory=playerTeams(playerId);
        TeamHistory current=teamHistory.stream().filter(TeamHistory::current).findFirst().orElse(teamHistory.isEmpty()?null:teamHistory.get(0));
        List<PlayerStats> stats=playerStats(playerId,seasonId,null,null); FootballTeamSeasonPlayer roster=current==null?null:rosters.selectOne(new QueryWrapper<FootballTeamSeasonPlayer>().eq("player_id",playerId).eq("team_id",current.teamId()).eq("season_id",current.seasonId()).eq("is_deleted",0).last("LIMIT 1"));
        List<TeamLink> links=playerTeamLinks(playerId); TeamLink club=links.stream().filter(x->"CLUB".equals(x.teamType())).findFirst().orElse(null);
        TeamLink national=links.stream().filter(x->"NATIONAL".equals(x.teamType())).findFirst().orElse(null);
        if(club==null&&current!=null)club=new TeamLink(current.teamId(),current.teamName(),current.teamLogoUrl(),"CLUB",current.shirtNumber());
        boolean retired=Objects.equals(p.getRetired(),1);
        return new PlayerOverview(p.getId(),p.getPlayerName(),p.getPlayerNameEn(),p.getAvatarUrl(),p.getPosition(),p.getNationality(),p.getBirthDate(),p.getBirthDate()==null?null:Period.between(p.getBirthDate(),LocalDate.now()).getYears(),p.getHeightCm(),p.getWeightKg(),null,current==null?null:current.teamId(),current==null?null:current.teamName(),current==null?null:current.teamLogoUrl(),current==null?p.getShirtNumber():current.shirtNumber(),roster!=null&&Objects.equals(roster.getCaptainFlag(),1),isFollowed("PLAYER",playerId),stats,playerCareer(playerId),recentContents("PLAYER",playerId),retired,retired?"RETIRED":"ACTIVE",club,national,retired?List.of():playerMatches(playerId,1,5).getRecords());
    }

    public PageResult<DetailMatch> teamMatches(Long teamId,String matchStatus,long pageNum,long pageSize){
        requireTeam(teamId); QueryWrapper<MatchInfo> q=new QueryWrapper<MatchInfo>()
                .and(w->w.eq("home_team_id",teamId).or().eq("away_team_id",teamId))
                .eq("status",ACTIVE).eq("is_deleted",0);
        boolean upcoming=false;
        if(StringUtils.hasText(matchStatus)){
            String value=matchStatus.toUpperCase();
            if("RECENT".equals(value))q.eq("match_status","FINISHED");
            else if("UPCOMING".equals(value)){q.in("match_status",List.of("SCHEDULED","LIVE"));upcoming=true;}
            else q.eq("match_status",value);
        }
        if(upcoming)q.orderByAsc("match_time").orderByAsc("id");else q.orderByDesc("match_time").orderByDesc("id");
        q.last("LIMIT 200");
        return detailMatchPage(matches.selectList(q),Map.of(),teamId,pageNum,pageSize);
    }

    public PageResult<ContentSummary> teamContents(Long teamId,String contentType,long pageNum,long pageSize){
        requireTeam(teamId);return relatedContents("TEAM",teamId,contentType,pageNum,pageSize);
    }

    public PageResult<ContentSummary> playerContents(Long playerId,String contentType,long pageNum,long pageSize){
        requirePlayer(playerId);return relatedContents("PLAYER",playerId,contentType,pageNum,pageSize);
    }

    public PageResult<DetailMatch> playerMatches(Long playerId,long pageNum,long pageSize){
        requirePlayer(playerId);
        if(matchPlayerStats==null||matchAppearances==null)return PageResult.of(List.of(),0,Math.max(1,pageNum),Math.min(100,Math.max(1,pageSize)));
        List<FootballMatchPlayerStat> statRows=matchPlayerStats.selectList(new QueryWrapper<FootballMatchPlayerStat>()
                .eq("player_id",playerId).eq("is_deleted",0).orderByDesc("match_id").last("LIMIT 200"));
        if(statRows.isEmpty())return PageResult.of(List.of(),0,Math.max(1,pageNum),Math.min(100,Math.max(1,pageSize)));
        Set<Long> matchIds=statRows.stream().map(FootballMatchPlayerStat::getMatchId).collect(Collectors.toCollection(LinkedHashSet::new));
        List<MatchInfo> matchRows=matches.selectBatchIds(matchIds).stream().filter(m->ACTIVE.equals(m.getStatus())&&Objects.equals(m.getIsDeleted(),0)).toList();
        Map<Long,FootballMatchPlayerAppearance> appearanceMap=matchAppearances.selectList(new QueryWrapper<FootballMatchPlayerAppearance>()
                .eq("player_id",playerId).in("match_id",matchIds).eq("status",ACTIVE).eq("is_deleted",0)).stream()
                .collect(Collectors.toMap(FootballMatchPlayerAppearance::getMatchId,Function.identity(),(a,b)->a));
        Map<Long,FootballMatchPlayerStat> statMap=statRows.stream().collect(Collectors.toMap(FootballMatchPlayerStat::getMatchId,Function.identity(),(a,b)->a));
        matchRows=new ArrayList<>(matchRows);matchRows.sort(Comparator.comparing(MatchInfo::getMatchTime,Comparator.nullsLast(Comparator.reverseOrder())).thenComparing(MatchInfo::getId));
        Map<Long,PlayerMatchContext> context=new HashMap<>();
        for(Long matchId:matchIds)context.put(matchId,new PlayerMatchContext(playerId,statMap.get(matchId),appearanceMap.get(matchId)));
        return detailMatchPage(matchRows,context,null,pageNum,pageSize);
    }

    public List<PlayerStats> playerStats(Long playerId,Long seasonId,Long leagueId,Long stageId){
        requirePlayer(playerId); QueryWrapper<FootballPlayerCompetitionStat> q=new QueryWrapper<FootballPlayerCompetitionStat>().eq("player_id",playerId).eq("is_deleted",0);
        if(seasonId!=null)q.eq("season_id",seasonId); if(leagueId!=null)q.eq("league_id",leagueId); if(stageId!=null)q.eq("stage_id",stageId);
        q.orderByDesc("season_id").orderByAsc("league_id").orderByAsc("stage_id"); List<FootballPlayerCompetitionStat> rows=playerStats.selectList(q);
        Set<Long> leagueIds=rows.stream().map(FootballPlayerCompetitionStat::getLeagueId).collect(Collectors.toSet());
        Set<Long> seasonIds=rows.stream().map(FootballPlayerCompetitionStat::getSeasonId).collect(Collectors.toSet());
        Set<Long> teamIds=rows.stream().map(FootballPlayerCompetitionStat::getTeamId).collect(Collectors.toSet());
        Map<Long,FootballLeague> lm=leagueIds.isEmpty()?Map.of():batch(leagues.selectBatchIds(leagueIds),FootballLeague::getId);
        Map<Long,FootballSeason> sm=seasonIds.isEmpty()?Map.of():batch(seasons.selectBatchIds(seasonIds),FootballSeason::getId);
        Map<Long,FootballTeam> tm=teamIds.isEmpty()?Map.of():batch(teams.selectBatchIds(teamIds),FootballTeam::getId);
        return rows.stream().map(s->new PlayerStats(s.getLeagueId(),name(lm.get(s.getLeagueId())),s.getSeasonId(),sm.get(s.getSeasonId())==null?null:sm.get(s.getSeasonId()).getSeasonName(),s.getTeamId(),teamName(tm.get(s.getTeamId())),nz(s.getAppearances()),nz(s.getStarts()),nz(s.getMinutes()),nz(s.getGoals()),nz(s.getAssists()),nz(s.getYellowCards()),nz(s.getRedCards()),nz(s.getShots()),nz(s.getShotsOnTarget()),ratio(s.getShotsOnTarget(),s.getShots()),s.getRating(),nz(s.getSaves()),s.getSource(),s.getSourceUpdatedAt())).toList();
    }

    public List<TeamHistory> playerTeams(Long playerId){
        requirePlayer(playerId); List<FootballPlayerTeamHistory> rows=histories.selectList(new QueryWrapper<FootballPlayerTeamHistory>().eq("player_id",playerId).eq("is_deleted",0).orderByDesc("current_flag").orderByDesc("start_date").orderByAsc("team_id"));
        Set<Long> teamIds=rows.stream().map(FootballPlayerTeamHistory::getTeamId).collect(Collectors.toSet());Set<Long> seasonIds=rows.stream().map(FootballPlayerTeamHistory::getSeasonId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long,FootballTeam> tm=teamIds.isEmpty()?Map.of():batch(teams.selectBatchIds(teamIds),FootballTeam::getId);
        Map<Long,FootballSeason> sm=seasonIds.isEmpty()?Map.of():batch(seasons.selectBatchIds(seasonIds),FootballSeason::getId);
        return rows.stream().map(h->{FootballTeam t=tm.get(h.getTeamId());FootballSeason s=sm.get(h.getSeasonId());return new TeamHistory(h.getTeamId(),teamName(t),t==null?null:t.getLogoUrl(),h.getSeasonId(),s==null?null:s.getSeasonName(),h.getStartDate(),h.getEndDate(),h.getShirtNumber(),h.getPosition(),nz(h.getAppearances()),nz(h.getGoals()),nz(h.getAssists()),Objects.equals(h.getCurrentFlag(),1),Objects.equals(h.getLoanFlag(),1));}).toList();
    }

    public Career playerCareer(Long playerId){
        requirePlayer(playerId); List<FootballPlayerCompetitionStat> rows=playerStats.selectList(new QueryWrapper<FootballPlayerCompetitionStat>().eq("player_id",playerId).eq("is_deleted",0));
        Set<Long> seasonIds=rows.stream().map(FootballPlayerCompetitionStat::getSeasonId).collect(Collectors.toSet()),teamIds=rows.stream().map(FootballPlayerCompetitionStat::getTeamId).collect(Collectors.toSet());
        Map<Long,FootballSeason> sm=seasonIds.isEmpty()?Map.of():batch(seasons.selectBatchIds(seasonIds),FootballSeason::getId); Map<Long,FootballTeam> tm=teamIds.isEmpty()?Map.of():batch(teams.selectBatchIds(teamIds),FootballTeam::getId);
        List<CareerGroup> bySeason=groups(rows,FootballPlayerCompetitionStat::getSeasonId,id->sm.get(id)==null?null:sm.get(id).getSeasonName());
        List<CareerGroup> byTeam=groups(rows,FootballPlayerCompetitionStat::getTeamId,id->teamName(tm.get(id)));
        return new Career(sum(rows,FootballPlayerCompetitionStat::getAppearances),sum(rows,FootballPlayerCompetitionStat::getStarts),sum(rows,FootballPlayerCompetitionStat::getMinutes),sum(rows,FootballPlayerCompetitionStat::getGoals),sum(rows,FootballPlayerCompetitionStat::getAssists),sum(rows,FootballPlayerCompetitionStat::getYellowCards),sum(rows,FootballPlayerCompetitionStat::getRedCards),sum(rows,FootballPlayerCompetitionStat::getShots),sum(rows,FootballPlayerCompetitionStat::getShotsOnTarget),average(rows),sum(rows,FootballPlayerCompetitionStat::getSaves),teamIds.size(),seasonIds.size(),bySeason,byTeam);
    }

    private Scope resolveTeamScope(Long teamId,Long requested){
        List<FootballTeamSeasonPlayer> rows=rosters.selectList(new QueryWrapper<FootballTeamSeasonPlayer>().eq("team_id",teamId).eq("status",ACTIVE).eq("is_deleted",0));
        if(requested!=null){FootballSeason s=seasons.selectById(requested);if(s==null||rows.stream().noneMatch(r->requested.equals(r.getSeasonId())))throw new BusinessException(ErrorCode.PARAM_ERROR,"season does not belong to team");return new Scope(s);}
        Set<Long> ids=rows.stream().map(FootballTeamSeasonPlayer::getSeasonId).collect(Collectors.toSet()); if(ids.isEmpty())throw new BusinessException(ErrorCode.NOT_FOUND,"team season not found");List<FootballSeason> candidates=seasons.selectBatchIds(ids);
        FootballSeason s=candidates.stream().max(Comparator.comparingInt((FootballSeason x)->Objects.equals(x.getCurrentFlag(),1)?1:0).thenComparing(FootballSeason::getStartDate)).orElseThrow(()->new BusinessException(ErrorCode.NOT_FOUND,"team season not found"));
        return new Scope(s);
    }
    private Standing standing(Long teamId,Long seasonId,Long stageId){FootballStanding s=standingEntity(teamId,seasonId,stageId);return s==null?null:new Standing(s.getRankNo(),s.getPlayed(),s.getWon(),s.getDrawn(),s.getLost(),s.getGoalsFor(),s.getGoalsAgainst(),s.getGoalDifference(),s.getPoints());}
    private FootballStanding standingEntity(Long teamId,Long seasonId,Long stageId){QueryWrapper<FootballStanding> q=new QueryWrapper<FootballStanding>().eq("team_id",teamId).eq("season_id",seasonId).eq("is_deleted",0);if(stageId!=null)q.eq("stage_id",stageId);else q.orderByAsc("stage_id").last("LIMIT 1");return standings.selectOne(q);}
    private List<Match> teamMatches(Long teamId,List<String> statuses,boolean asc,int limit){QueryWrapper<MatchInfo> q=new QueryWrapper<MatchInfo>().and(w->w.eq("home_team_id",teamId).or().eq("away_team_id",teamId)).in("match_status",statuses).eq("status",ACTIVE).eq("is_deleted",0);if(asc)q.orderByAsc("match_time");else q.orderByDesc("match_time");q.last("LIMIT "+limit);List<MatchInfo> ms=matches.selectList(q);Set<Long> ids=ms.stream().flatMap(m->java.util.stream.Stream.of(m.getHomeTeamId(),m.getAwayTeamId())).collect(Collectors.toSet());Map<Long,FootballTeam> tm=ids.isEmpty()?Map.of():batch(teams.selectBatchIds(ids),FootballTeam::getId);return ms.stream().map(m->new Match(m.getId(),m.getLeagueId(),m.getHomeTeamId(),teamName(tm.get(m.getHomeTeamId())),m.getAwayTeamId(),teamName(tm.get(m.getAwayTeamId())),m.getHomeScore(),m.getAwayScore(),m.getMatchStatus(),m.getMatchTime())).toList();}
    private List<ContentSummary> recentContents(String type,Long id){return relatedContents(type,id,null,1,10).getRecords();}

    private PageResult<ContentSummary> relatedContents(String relationType,Long relationId,String contentType,long pageNum,long pageSize){
        List<ContentRelation> rs=contentRelations.selectList(new QueryWrapper<ContentRelation>()
                .eq("relation_type",relationType).eq("relation_id",relationId).eq("status",ACTIVE).eq("is_deleted",0)
                .orderByDesc("id").last("LIMIT 200"));
        long pn=Math.max(1,pageNum),ps=Math.min(100,Math.max(1,pageSize));
        if(rs.isEmpty())return PageResult.of(List.of(),0,pn,ps);
        QueryWrapper<Content> q=new QueryWrapper<Content>().in("id",rs.stream().map(ContentRelation::getContentId).toList())
                .eq("status","PUBLISHED").eq("is_deleted",0);
        if(StringUtils.hasText(contentType))q.eq("content_type",contentType.toUpperCase());
        List<Content> values=new ArrayList<>(contents.selectList(q));
        values.sort(Comparator.comparing(Content::getHotScore,Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(Content::getPublishTime,Comparator.nullsLast(Comparator.reverseOrder())).thenComparing(Content::getId));
        int from=(int)Math.min((pn-1)*ps,values.size()),to=(int)Math.min(from+ps,values.size());
        List<ContentSummary> records=values.subList(from,to).stream().map(c->new ContentSummary(c.getId(),c.getContentType(),
                c.getTitle(),c.getSummary(),c.getCoverUrl(),c.getPublishTime(),nz(c.getLikeCount()),nz(c.getCommentCount()),nz(c.getFavoriteCount()))).toList();
        return PageResult.of(records,values.size(),pn,ps);
    }

    private List<CompetitionStanding> competitionStandings(Long teamId){
        List<FootballStanding> rows=standings.selectList(new QueryWrapper<FootballStanding>().eq("team_id",teamId)
                .eq("is_deleted",0).orderByDesc("season_id").orderByAsc("league_id").orderByAsc("stage_id").last("LIMIT 20"));
        Set<Long> leagueIds=rows.stream().map(FootballStanding::getLeagueId).collect(Collectors.toSet());
        Set<Long> seasonIds=rows.stream().map(FootballStanding::getSeasonId).collect(Collectors.toSet());
        Map<Long,FootballLeague> lm=leagueIds.isEmpty()?Map.of():batch(leagues.selectBatchIds(leagueIds),FootballLeague::getId);
        Map<Long,FootballSeason> sm=seasonIds.isEmpty()?Map.of():batch(seasons.selectBatchIds(seasonIds),FootballSeason::getId);
        return rows.stream().map(s->new CompetitionStanding(s.getLeagueId(),name(lm.get(s.getLeagueId())),s.getSeasonId(),
                sm.get(s.getSeasonId())==null?null:sm.get(s.getSeasonId()).getSeasonName(),s.getStageId(),s.getRankNo(),
                s.getPlayed(),s.getWon(),s.getDrawn(),s.getLost(),s.getGoalsFor(),s.getGoalsAgainst(),s.getGoalDifference(),s.getPoints())).toList();
    }

    private List<Leaderboard> leaderboards(List<RosterPlayer> squad){
        Comparator<RosterPlayer> goals=Comparator.comparingInt((RosterPlayer x)->nz(x.goals())).reversed().thenComparing(RosterPlayer::playerId);
        Comparator<RosterPlayer> assists=Comparator.comparingInt((RosterPlayer x)->nz(x.assists())).reversed().thenComparing(RosterPlayer::playerId);
        Comparator<RosterPlayer> appearances=Comparator.comparingInt((RosterPlayer x)->nz(x.appearances())).reversed().thenComparing(RosterPlayer::playerId);
        Comparator<RosterPlayer> ratings=Comparator.comparing(RosterPlayer::rating,Comparator.nullsLast(Comparator.reverseOrder())).thenComparing(RosterPlayer::playerId);
        return List.of(new Leaderboard("GOALS","射手榜",squad.stream().sorted(goals).limit(5).toList()),
                new Leaderboard("ASSISTS","助攻榜",squad.stream().sorted(assists).limit(5).toList()),
                new Leaderboard("APPEARANCES","出场榜",squad.stream().sorted(appearances).limit(5).toList()),
                new Leaderboard("RATING","评分榜",squad.stream().sorted(ratings).limit(5).toList()));
    }

    private List<TeamLink> playerTeamLinks(Long playerId){
        if(teamPlayers==null)return List.of();
        List<TeamPlayer> rows=teamPlayers.selectList(new QueryWrapper<TeamPlayer>().eq("player_id",playerId)
                .eq("status",ACTIVE).eq("is_deleted",0).orderByDesc("season").orderByAsc("team_id"));
        Set<Long> ids=rows.stream().map(TeamPlayer::getTeamId).collect(Collectors.toCollection(LinkedHashSet::new));
        Map<Long,FootballTeam> tm=ids.isEmpty()?Map.of():batch(teams.selectBatchIds(ids),FootballTeam::getId);
        Map<String,TeamLink> unique=new LinkedHashMap<>();
        for(TeamPlayer row:rows){FootballTeam team=tm.get(row.getTeamId());String raw=Objects.toString(row.getTeamType(),"CLUB").toUpperCase();
            String type=raw.contains("NATIONAL")?"NATIONAL":"CLUB";unique.putIfAbsent(type,new TeamLink(row.getTeamId(),teamName(team),team==null?null:team.getLogoUrl(),type,row.getShirtNumber()));}
        return new ArrayList<>(unique.values());
    }

    private PageResult<DetailMatch> detailMatchPage(List<MatchInfo> rows,Map<Long,PlayerMatchContext> contexts,Long detailTeamId,long pageNum,long pageSize){
        long pn=Math.max(1,pageNum),ps=Math.min(100,Math.max(1,pageSize));int from=(int)Math.min((pn-1)*ps,rows.size()),to=(int)Math.min(from+ps,rows.size());
        List<MatchInfo> pageRows=rows.subList(from,to);
        Set<Long> teamIds=pageRows.stream().flatMap(m->java.util.stream.Stream.of(m.getHomeTeamId(),m.getAwayTeamId())).filter(Objects::nonNull).collect(Collectors.toSet());
        Set<Long> leagueIds=pageRows.stream().map(MatchInfo::getLeagueId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long,FootballTeam> tm=teamIds.isEmpty()?Map.of():batch(teams.selectBatchIds(teamIds),FootballTeam::getId);
        Map<Long,FootballLeague> lm=leagueIds.isEmpty()?Map.of():batch(leagues.selectBatchIds(leagueIds),FootballLeague::getId);
        List<DetailMatch> result=pageRows.stream().map(m->{FootballTeam home=tm.get(m.getHomeTeamId()),away=tm.get(m.getAwayTeamId());PlayerMatchContext c=contexts.get(m.getId());
            FootballMatchPlayerStat s=c==null?null:c.stat();FootballMatchPlayerAppearance a=c==null?null:c.appearance();Long owner=s==null?detailTeamId:s.getTeamId();
            return new DetailMatch(m.getId(),m.getLeagueId(),name(lm.get(m.getLeagueId())),m.getRoundName(),m.getMatchTime(),m.getMatchStatus(),m.getVenue(),
                    m.getHomeTeamId(),teamName(home),home==null?null:home.getLogoUrl(),m.getHomeScore(),m.getAwayTeamId(),teamName(away),away==null?null:away.getLogoUrl(),m.getAwayScore(),
                    owner,a==null?null:Objects.equals(a.getStartedFlag(),1),s==null?null:s.getMinutes(),s==null?null:s.getGoals(),s==null?null:s.getAssists(),s==null?null:s.getOfficialRating());}).toList();
        return PageResult.of(result,rows.size(),pn,ps);
    }
    private RosterPlayer rosterPlayer(FootballTeamSeasonPlayer r,FootballPlayer p,FootballPlayerCompetitionStat s,FootballTeam loan,boolean followed){return new RosterPlayer(r.getPlayerId(),p==null?null:p.getPlayerName(),p==null?null:p.getPlayerNameEn(),p==null?null:p.getAvatarUrl(),r.getPosition(),r.getShirtNumber(),Objects.equals(r.getCaptainFlag(),1),Objects.equals(r.getLoanFlag(),1),r.getLoanFromTeamId(),teamName(loan),r.getSquadRole(),s==null?0:nz(s.getAppearances()),s==null?0:nz(s.getStarts()),s==null?0:nz(s.getMinutes()),s==null?0:nz(s.getGoals()),s==null?0:nz(s.getAssists()),s==null?null:s.getRating(),followed);}
    private List<CareerGroup> groups(List<FootballPlayerCompetitionStat> rows,Function<FootballPlayerCompetitionStat,Long> key,Function<Long,String> name){Map<Long,List<FootballPlayerCompetitionStat>> grouped=rows.stream().collect(Collectors.groupingBy(key,LinkedHashMap::new,Collectors.toList()));return grouped.entrySet().stream().map(e->new CareerGroup(e.getKey(),name.apply(e.getKey()),sum(e.getValue(),FootballPlayerCompetitionStat::getAppearances),sum(e.getValue(),FootballPlayerCompetitionStat::getStarts),sum(e.getValue(),FootballPlayerCompetitionStat::getMinutes),sum(e.getValue(),FootballPlayerCompetitionStat::getGoals),sum(e.getValue(),FootballPlayerCompetitionStat::getAssists),average(e.getValue()))).toList();}
    private int sum(List<FootballPlayerCompetitionStat> rows,Function<FootballPlayerCompetitionStat,Integer> f){return rows.stream().map(f).filter(Objects::nonNull).mapToInt(Integer::intValue).sum();}
    private BigDecimal average(List<FootballPlayerCompetitionStat> rows){int weight=rows.stream().filter(x->x.getRating()!=null).mapToInt(x->Math.max(1,nz(x.getAppearances()))).sum();if(weight==0)return BigDecimal.ZERO;BigDecimal total=rows.stream().filter(x->x.getRating()!=null).map(x->x.getRating().multiply(BigDecimal.valueOf(Math.max(1,nz(x.getAppearances()))))).reduce(BigDecimal.ZERO,BigDecimal::add);return total.divide(BigDecimal.valueOf(weight),2,RoundingMode.HALF_UP);}
    private Set<Long> followedIds(String type,Set<Long> targetIds){Long uid=userId();if(uid==null||targetIds.isEmpty())return Set.of();return follows.selectList(new QueryWrapper<FollowRecord>().eq("user_id",uid).eq("follow_type",type).in("target_id",targetIds).eq("status",ACTIVE).eq("is_deleted",0)).stream().map(FollowRecord::getTargetId).collect(Collectors.toSet());}
    private boolean isFollowed(String type,Long id){return followedIds(type,Set.of(id)).contains(id);}
    private Long userId(){Authentication a=SecurityContextHolder.getContext().getAuthentication();return a!=null&&a.getPrincipal() instanceof LoginUserContext c?c.getUserId():null;}
    private FootballTeam requireTeam(Long id){FootballTeam t=teams.selectById(id);if(t==null||!ACTIVE.equals(t.getStatus())||!Objects.equals(t.getIsDeleted(),0))throw new BusinessException(ErrorCode.NOT_FOUND,"team not found");return t;}
    private FootballPlayer requirePlayer(Long id){FootballPlayer p=players.selectById(id);if(p==null||!ACTIVE.equals(p.getStatus())||!Objects.equals(p.getIsDeleted(),0))throw new BusinessException(ErrorCode.NOT_FOUND,"player not found");return p;}
    private static <T> Map<Long,T> batch(List<T> rows,Function<T,Long> id){if(rows==null)return Map.of();return rows.stream().collect(Collectors.toMap(id,Function.identity(),(a,b)->a));}
    private static int nz(Integer v){return v==null?0:v;} private static BigDecimal ratio(Integer n,Integer d){return nz(d)==0?BigDecimal.ZERO:BigDecimal.valueOf(nz(n)*100.0/nz(d)).setScale(2,RoundingMode.HALF_UP);}
    private static String normalizePosition(String p){String x=p.toUpperCase();return switch(x){case "GK","GOALKEEPER"->"GOALKEEPER";case "DF","DEFENDER"->"DEFENDER";case "MF","MIDFIELDER"->"MIDFIELDER";case "FW","FORWARD"->"FORWARD";default->throw new BusinessException(ErrorCode.PARAM_ERROR,"invalid position");};}
    private static int positionOrder(String p){return switch(p){case "GOALKEEPER"->0;case "DEFENDER"->1;case "MIDFIELDER"->2;default->3;};}
    private static List<Integer> years(String s){if(!StringUtils.hasText(s))return List.of();List<Integer> out=new ArrayList<>();for(String v:s.split(","))try{out.add(Integer.valueOf(v.trim()));}catch(NumberFormatException ignored){}return out;}
    private static String name(FootballLeague x){return x==null?null:x.getLeagueName();} private static String teamName(FootballTeam x){return x==null?null:x.getTeamName();}
    private record Scope(FootballSeason season){}
    private record PlayerMatchContext(Long playerId,FootballMatchPlayerStat stat,FootballMatchPlayerAppearance appearance){}
}
