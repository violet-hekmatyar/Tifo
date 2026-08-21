package com.southstand.football.matchdata.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.southstand.auth.security.CurrentUserHolder;
import com.southstand.auth.security.LoginUserContext;
import com.southstand.common.enums.ErrorCode;
import com.southstand.common.exception.BusinessException;
import com.southstand.common.result.PageResult;
import com.southstand.football.match.entity.MatchInfo;
import com.southstand.football.match.mapper.MatchInfoMapper;
import com.southstand.football.match.vo.MatchDetailVO;
import com.southstand.football.matchdata.entity.FootballMatchLineup;
import com.southstand.football.matchdata.entity.FootballMatchPlayerAppearance;
import com.southstand.football.matchdata.entity.FootballMatchPlayerStat;
import com.southstand.football.matchdata.entity.FootballMatchTeamStat;
import com.southstand.football.matchdata.entity.FootballUserPlayerRating;
import com.southstand.football.matchdata.mapper.FootballMatchLineupMapper;
import com.southstand.football.matchdata.mapper.FootballMatchPlayerAppearanceMapper;
import com.southstand.football.matchdata.mapper.FootballMatchPlayerStatMapper;
import com.southstand.football.matchdata.mapper.FootballMatchTeamStatMapper;
import com.southstand.football.matchdata.mapper.FootballUserPlayerRatingMapper;
import com.southstand.football.matchdata.vo.MatchDataVO;
import com.southstand.football.matchdata.vo.MatchDataVO.LineupPlayer;
import com.southstand.football.matchdata.vo.MatchDataVO.ManOfTheMatch;
import com.southstand.football.matchdata.vo.MatchDataVO.PlayerStat;
import com.southstand.football.matchdata.vo.MatchDataVO.RatingResult;
import com.southstand.football.matchdata.vo.MatchDataVO.RatingSummary;
import com.southstand.football.matchdata.vo.MatchDataVO.TeamLineup;
import com.southstand.football.matchdata.vo.MatchDataVO.TeamStatItem;
import com.southstand.football.player.entity.FootballPlayer;
import com.southstand.football.player.mapper.FootballPlayerMapper;
import com.southstand.football.team.entity.FootballTeam;
import com.southstand.football.team.mapper.FootballTeamMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class MatchDataService {
    private static final String ACTIVE="ACTIVE";
    private static final int NOT_DELETED=0;
    private static final List<String> DISTRIBUTION_BUCKETS=List.of("1.0-2.0","2.5-4.0","4.5-6.0","6.5-8.0","8.5-10.0");

    private final MatchInfoMapper matches; private final FootballTeamMapper teams; private final FootballPlayerMapper players;
    private final FootballMatchLineupMapper lineups; private final FootballMatchPlayerAppearanceMapper appearances;
    private final FootballMatchTeamStatMapper teamStats; private final FootballMatchPlayerStatMapper playerStats; private final FootballUserPlayerRatingMapper ratings;

    public MatchDataService(MatchInfoMapper matches,FootballTeamMapper teams,FootballPlayerMapper players,FootballMatchLineupMapper lineups,
            FootballMatchPlayerAppearanceMapper appearances,FootballMatchTeamStatMapper teamStats,FootballMatchPlayerStatMapper playerStats,FootballUserPlayerRatingMapper ratings){
        this.matches=matches;this.teams=teams;this.players=players;this.lineups=lineups;this.appearances=appearances;this.teamStats=teamStats;this.playerStats=playerStats;this.ratings=ratings;
    }

    public MatchDataVO.Lineups lineups(Long matchId){
        MatchInfo match=requireMatch(matchId);
        List<FootballMatchLineup> lineupRows=lineups.selectList(active("match_id",matchId));
        List<FootballMatchPlayerAppearance> appearanceRows=appearances.selectList(activeStatus("match_id",matchId));
        Map<Long,FootballMatchLineup> lineupMap=lineupRows.stream().collect(Collectors.toMap(FootballMatchLineup::getTeamId,Function.identity()));
        Map<Long,FootballTeam> teamMap=batchTeams(Set.of(match.getHomeTeamId(),match.getAwayTeamId()));
        Set<Long> playerIds=appearanceRows.stream().map(FootballMatchPlayerAppearance::getPlayerId).collect(Collectors.toCollection(LinkedHashSet::new));
        Map<Long,FootballPlayer> playerMap=batchPlayers(playerIds);
        return new MatchDataVO.Lineups(teamLineup(match.getHomeTeamId(),lineupMap.get(match.getHomeTeamId()),appearanceRows,teamMap,playerMap),
                teamLineup(match.getAwayTeamId(),lineupMap.get(match.getAwayTeamId()),appearanceRows,teamMap,playerMap));
    }

    public List<TeamStatItem> teamStats(Long matchId){
        MatchInfo match=requireMatch(matchId);
        Map<Long,FootballMatchTeamStat> map=teamStats.selectList(active("match_id",matchId)).stream().collect(Collectors.toMap(FootballMatchTeamStat::getTeamId,Function.identity()));
        FootballMatchTeamStat h=map.get(match.getHomeTeamId()),a=map.get(match.getAwayTeamId());
        if(h==null&&a==null)return List.of();
        return List.of(item("POSSESSION","控球率",h==null?null:h.getPossession(),a==null?null:a.getPossession(),"%"),
                item("SHOTS","射门",iv(h,FootballMatchTeamStat::getShots),iv(a,FootballMatchTeamStat::getShots),null),
                item("SHOTS_ON_TARGET","射正",iv(h,FootballMatchTeamStat::getShotsOnTarget),iv(a,FootballMatchTeamStat::getShotsOnTarget),null),
                item("CORNERS","角球",iv(h,FootballMatchTeamStat::getCorners),iv(a,FootballMatchTeamStat::getCorners),null),
                item("FOULS","犯规",iv(h,FootballMatchTeamStat::getFouls),iv(a,FootballMatchTeamStat::getFouls),null),
                item("OFFSIDES","越位",iv(h,FootballMatchTeamStat::getOffsides),iv(a,FootballMatchTeamStat::getOffsides),null),
                item("YELLOW_CARDS","黄牌",iv(h,FootballMatchTeamStat::getYellowCards),iv(a,FootballMatchTeamStat::getYellowCards),null),
                item("RED_CARDS","红牌",iv(h,FootballMatchTeamStat::getRedCards),iv(a,FootballMatchTeamStat::getRedCards),null),
                item("PASSES","传球",iv(h,FootballMatchTeamStat::getPasses),iv(a,FootballMatchTeamStat::getPasses),null),
                item("PASS_ACCURACY","传球成功率",h==null?null:h.getPassAccuracy(),a==null?null:a.getPassAccuracy(),"%"),
                item("SAVES","扑救",iv(h,FootballMatchTeamStat::getSaves),iv(a,FootballMatchTeamStat::getSaves),null),
                item("EXPECTED_GOALS","预期进球",h==null?null:h.getExpectedGoals(),a==null?null:a.getExpectedGoals(),null));
    }

    public PageResult<PlayerStat> playerStats(Long matchId,Long teamId,String position,long pageNum,long pageSize){
        MatchInfo match=requireMatch(matchId); validateTeam(match,teamId);
        List<FootballMatchPlayerStat> statRows=playerStats.selectList(active("match_id",matchId));
        List<FootballMatchPlayerAppearance> appearanceRows=appearances.selectList(activeStatus("match_id",matchId));
        Map<Long,FootballMatchPlayerAppearance> appearanceMap=appearanceRows.stream().collect(Collectors.toMap(FootballMatchPlayerAppearance::getPlayerId,Function.identity()));
        if(teamId!=null)statRows=statRows.stream().filter(x->teamId.equals(x.getTeamId())).toList();
        if(StringUtils.hasText(position)){String p=normalizePosition(position);statRows=statRows.stream().filter(x->{var ap=appearanceMap.get(x.getPlayerId());return ap!=null&&p.equals(ap.getPosition());}).toList();}
        Set<Long> playerIds=statRows.stream().map(FootballMatchPlayerStat::getPlayerId).collect(Collectors.toSet());
        Map<Long,FootballPlayer> playerMap=batchPlayers(playerIds); Map<Long,FootballTeam> teamMap=batchTeams(statRows.stream().map(FootballMatchPlayerStat::getTeamId).collect(Collectors.toSet()));
        List<FootballUserPlayerRating> ratingRows=ratingRows(matchId); Map<Long,RatingAggregate> aggregates=aggregate(ratingRows); Long userId=optionalUserId();
        List<PlayerStat> values=statRows.stream().map(x->toPlayerStat(x,appearanceMap.get(x.getPlayerId()),playerMap.get(x.getPlayerId()),teamMap.get(x.getTeamId()),aggregates.get(x.getPlayerId()),myRating(ratingRows,x.getPlayerId(),userId))).sorted(Comparator.comparing(PlayerStat::officialRating,Comparator.nullsLast(Comparator.reverseOrder())).thenComparing(PlayerStat::playerId)).toList();
        long pn=Math.max(1,pageNum),ps=Math.min(100,Math.max(1,pageSize));int from=(int)Math.min((pn-1)*ps,values.size()),to=(int)Math.min(from+ps,values.size());
        return PageResult.of(values.subList(from,to),values.size(),pn,ps);
    }

    @Transactional
    public RatingResult submitRating(Long matchId,Long playerId,BigDecimal rating){
        validateRating(rating);Long userId=CurrentUserHolder.get().getUserId();MatchInfo match=requireMatch(matchId);
        if(!"FINISHED".equals(match.getMatchStatus()))throw new BusinessException(ErrorCode.CONFLICT,"only finished matches can be rated");
        requireAppeared(matchId,playerId);
        FootballUserPlayerRating row=new FootballUserPlayerRating();row.setId(IdWorker.getId());row.setMatchId(matchId);row.setPlayerId(playerId);row.setUserId(userId);row.setRating(rating.setScale(1));
        ratings.upsertActive(row);return ratingResult(matchId,playerId,userId,rating.setScale(1));
    }

    @Transactional
    public RatingResult cancelRating(Long matchId,Long playerId){
        Long userId=CurrentUserHolder.get().getUserId();requireMatch(matchId);ratings.cancelOwn(userId,matchId,playerId);return ratingResult(matchId,playerId,userId,null);
    }

    public List<RatingSummary> ratings(Long matchId,Long teamId){
        MatchInfo match=requireMatch(matchId);validateTeam(match,teamId);
        List<FootballMatchPlayerAppearance> appeared=appearances.selectList(this.<FootballMatchPlayerAppearance>activeStatus("match_id",matchId).eq("appeared_flag",1));
        if(teamId!=null)appeared=appeared.stream().filter(x->teamId.equals(x.getTeamId())).toList();
        Set<Long> ids=appeared.stream().map(FootballMatchPlayerAppearance::getPlayerId).collect(Collectors.toSet());Map<Long,FootballPlayer> playerMap=batchPlayers(ids);
        Map<Long,FootballMatchPlayerStat> statMap=playerStats.selectList(active("match_id",matchId)).stream().collect(Collectors.toMap(FootballMatchPlayerStat::getPlayerId,Function.identity()));
        List<FootballUserPlayerRating> ratingRows=ratingRows(matchId);Map<Long,RatingAggregate> aggregates=aggregate(ratingRows);Long userId=optionalUserId();
        return appeared.stream().sorted(Comparator.comparing(FootballMatchPlayerAppearance::getTeamId).thenComparing(FootballMatchPlayerAppearance::getShirtNumber,Comparator.nullsLast(Integer::compareTo))).map(ap->{
            FootballPlayer p=playerMap.get(ap.getPlayerId());FootballMatchPlayerStat s=statMap.get(ap.getPlayerId());RatingAggregate ag=aggregates.get(ap.getPlayerId());
            return new RatingSummary(ap.getPlayerId(),p==null?null:p.getPlayerName(),ap.getTeamId(),s==null?null:s.getOfficialRating(),ag==null?null:ag.average(),ag==null?0:ag.count(),myRating(ratingRows,ap.getPlayerId(),userId),ag==null?emptyDistribution():ag.distribution());
        }).toList();
    }

    public void enhance(MatchDetailVO detail){
        Long matchId=detail.getMatchId();List<FootballMatchLineup> ls=lineups.selectList(active("match_id",matchId));List<FootballMatchTeamStat> ts=teamStats.selectList(active("match_id",matchId));
        List<FootballMatchPlayerStat> ps=playerStats.selectList(active("match_id",matchId));List<FootballUserPlayerRating> rs=ratingRows(matchId);
        detail.setLineupsAvailable(!ls.isEmpty());detail.setTeamStatsAvailable(!ts.isEmpty());detail.setPlayerStatsAvailable(!ps.isEmpty());detail.setRatingsAvailable(!rs.isEmpty());
        detail.setRatingUserCount((int)rs.stream().map(FootballUserPlayerRating::getUserId).distinct().count());detail.setManOfTheMatch(manOfTheMatch(ps,rs));
    }

    private RatingResult ratingResult(Long matchId,Long playerId,Long userId,BigDecimal my){List<FootballUserPlayerRating> rows=ratingRows(matchId).stream().filter(x->playerId.equals(x.getPlayerId())).toList();RatingAggregate ag=aggregateOne(rows);return new RatingResult(matchId,playerId,my,ag.count()==0?null:ag.average(),ag.count(),LocalDateTime.now());}
    private void requireAppeared(Long matchId,Long playerId){Long count=appearances.selectCount(this.<FootballMatchPlayerAppearance>activeStatus("match_id",matchId).eq("player_id",playerId).eq("appeared_flag",1));if(count==null||count==0)throw new BusinessException(ErrorCode.CONFLICT,"player did not appear in this match");}
    private MatchInfo requireMatch(Long id){MatchInfo m=matches.selectById(id);if(m==null||!ACTIVE.equals(m.getStatus())||!Objects.equals(m.getIsDeleted(),0))throw new BusinessException(ErrorCode.NOT_FOUND,"match not found");return m;}
    private void validateTeam(MatchInfo m,Long teamId){if(teamId!=null&&!teamId.equals(m.getHomeTeamId())&&!teamId.equals(m.getAwayTeamId()))throw new BusinessException(ErrorCode.PARAM_ERROR,"team is not part of match");}
    private void validateRating(BigDecimal rating){if(rating==null||rating.compareTo(BigDecimal.ONE)<0||rating.compareTo(BigDecimal.TEN)>0||rating.multiply(BigDecimal.valueOf(2)).stripTrailingZeros().scale()>0)throw new BusinessException(ErrorCode.PARAM_ERROR,"rating must be 1.0-10.0 in 0.5 steps");}

    private TeamLineup teamLineup(Long teamId,FootballMatchLineup lineup,List<FootballMatchPlayerAppearance> all,Map<Long,FootballTeam> teamMap,Map<Long,FootballPlayer> playerMap){
        List<FootballMatchPlayerAppearance> rows=all.stream().filter(x->teamId.equals(x.getTeamId())).sorted(Comparator.comparingInt(this::lineupOrder).thenComparing(FootballMatchPlayerAppearance::getShirtNumber,Comparator.nullsLast(Integer::compareTo)).thenComparing(FootballMatchPlayerAppearance::getPlayerId)).toList();
        List<LineupPlayer> starters=new ArrayList<>(),subs=new ArrayList<>(),bench=new ArrayList<>();for(var ap:rows){LineupPlayer p=toLineupPlayer(ap,playerMap.get(ap.getPlayerId()));if("STARTER".equals(ap.getLineupType()))starters.add(p);else if("SUBSTITUTE".equals(ap.getLineupType()))subs.add(p);else bench.add(p);}FootballTeam team=teamMap.get(teamId);
        return new TeamLineup(teamId,team==null?null:team.getTeamName(),team==null?null:team.getLogoUrl(),lineup==null?null:lineup.getFormation(),lineup==null?null:lineup.getCoachName(),starters,subs,bench);
    }
    private LineupPlayer toLineupPlayer(FootballMatchPlayerAppearance a,FootballPlayer p){return new LineupPlayer(a.getPlayerId(),p==null?null:p.getPlayerName(),p==null?null:p.getAvatarUrl(),a.getPosition(),a.getShirtNumber(),a.getCaptainFlag()==1,a.getStartedFlag()==1,a.getAppearedFlag()==1,a.getSubstitutedInMinute(),a.getSubstitutedOutMinute(),a.getFieldX(),a.getFieldY());}
    private PlayerStat toPlayerStat(FootballMatchPlayerStat s,FootballMatchPlayerAppearance a,FootballPlayer p,FootballTeam t,RatingAggregate ag,BigDecimal my){BigDecimal accuracy=s.getPasses()==null||s.getPasses()==0?BigDecimal.ZERO:BigDecimal.valueOf(s.getSuccessfulPasses()).multiply(BigDecimal.valueOf(100)).divide(BigDecimal.valueOf(s.getPasses()),2,RoundingMode.HALF_UP);return new PlayerStat(s.getPlayerId(),p==null?null:p.getPlayerName(),p==null?null:p.getAvatarUrl(),s.getTeamId(),t==null?null:t.getTeamName(),a==null?null:a.getPosition(),a==null?null:a.getShirtNumber(),a!=null&&a.getStartedFlag()==1,a!=null&&a.getCaptainFlag()==1,s.getMinutes(),s.getGoals(),s.getAssists(),s.getShots(),s.getShotsOnTarget(),s.getPasses(),s.getSuccessfulPasses(),accuracy,s.getKeyPasses(),s.getTackles(),s.getInterceptions(),s.getSaves(),s.getYellowCards(),s.getRedCards(),s.getOfficialRating(),ag==null?null:ag.average(),ag==null?0:ag.count(),my);}
    private ManOfTheMatch manOfTheMatch(List<FootballMatchPlayerStat> stats,List<FootballUserPlayerRating> rows){Map<Long,RatingAggregate> ag=aggregate(rows);FootballMatchPlayerStat best=null;BigDecimal score=null;String source=null;int count=0;for(var s:stats){RatingAggregate a=ag.get(s.getPlayerId());BigDecimal candidate=a!=null&&a.count()>=3?a.average():s.getOfficialRating();if(candidate!=null&&(score==null||candidate.compareTo(score)>0||(candidate.compareTo(score)==0&&s.getPlayerId()<best.getPlayerId()))){best=s;score=candidate;source=a!=null&&a.count()>=3?"USER":"OFFICIAL";count=a==null?0:a.count();}}if(best==null)return null;FootballPlayer p=players.selectById(best.getPlayerId());return new ManOfTheMatch(best.getPlayerId(),p==null?null:p.getPlayerName(),best.getTeamId(),score,source,count);}

    private List<FootballUserPlayerRating> ratingRows(Long matchId){return ratings.selectList(activeStatus("match_id",matchId));}
    private Map<Long,RatingAggregate> aggregate(List<FootballUserPlayerRating> rows){return rows.stream().collect(Collectors.groupingBy(FootballUserPlayerRating::getPlayerId,LinkedHashMap::new,Collectors.collectingAndThen(Collectors.toList(),this::aggregateOne)));}
    private RatingAggregate aggregateOne(List<FootballUserPlayerRating> rows){if(rows.isEmpty())return new RatingAggregate(null,0,emptyDistribution());BigDecimal sum=rows.stream().map(FootballUserPlayerRating::getRating).reduce(BigDecimal.ZERO,BigDecimal::add);Map<String,Integer>d=emptyDistribution();for(var r:rows){String b=bucket(r.getRating());d.put(b,d.get(b)+1);}return new RatingAggregate(sum.divide(BigDecimal.valueOf(rows.size()),2,RoundingMode.HALF_UP),rows.size(),d);}
    private Map<String,Integer> emptyDistribution(){Map<String,Integer> d=new LinkedHashMap<>();DISTRIBUTION_BUCKETS.forEach(x->d.put(x,0));return d;}
    private String bucket(BigDecimal r){if(r.compareTo(BigDecimal.valueOf(2))<=0)return DISTRIBUTION_BUCKETS.get(0);if(r.compareTo(BigDecimal.valueOf(4))<=0)return DISTRIBUTION_BUCKETS.get(1);if(r.compareTo(BigDecimal.valueOf(6))<=0)return DISTRIBUTION_BUCKETS.get(2);if(r.compareTo(BigDecimal.valueOf(8))<=0)return DISTRIBUTION_BUCKETS.get(3);return DISTRIBUTION_BUCKETS.get(4);}
    private BigDecimal myRating(List<FootballUserPlayerRating> rows,Long playerId,Long userId){if(userId==null)return null;return rows.stream().filter(x->playerId.equals(x.getPlayerId())&&userId.equals(x.getUserId())).map(FootballUserPlayerRating::getRating).findFirst().orElse(null);}
    private Long optionalUserId(){Authentication a=SecurityContextHolder.getContext().getAuthentication();return a!=null&&a.getPrincipal() instanceof LoginUserContext c?c.getUserId():null;}
    private Map<Long,FootballPlayer> batchPlayers(Set<Long> ids){if(ids.isEmpty())return Collections.emptyMap();return players.selectBatchIds(ids).stream().collect(Collectors.toMap(FootballPlayer::getId,Function.identity()));}
    private Map<Long,FootballTeam> batchTeams(Set<Long> ids){if(ids.isEmpty())return Collections.emptyMap();return teams.selectBatchIds(ids).stream().collect(Collectors.toMap(FootballTeam::getId,Function.identity()));}
    private <T> QueryWrapper<T> active(String field,Object value){return new QueryWrapper<T>().eq(field,value).eq("is_deleted",NOT_DELETED);}
    private <T> QueryWrapper<T> activeStatus(String field,Object value){return this.<T>active(field,value).eq("status",ACTIVE);}
    private TeamStatItem item(String type,String name,Object h,Object a,String unit){return new TeamStatItem(type,name,h,a,unit);}
    private Object iv(FootballMatchTeamStat s,Function<FootballMatchTeamStat,Integer> f){return s==null?null:f.apply(s);}
    private int lineupOrder(FootballMatchPlayerAppearance a){return switch(a.getLineupType()){case "STARTER"->0;case "SUBSTITUTE"->1;default->2;};}
    private String normalizePosition(String p){String x=p.toUpperCase();return switch(x){case "GK","GOALKEEPER"->"GOALKEEPER";case "DF","DEFENDER"->"DEFENDER";case "MF","MIDFIELDER"->"MIDFIELDER";case "FW","FORWARD"->"FORWARD";default->throw new BusinessException(ErrorCode.PARAM_ERROR,"invalid position");};}
    private record RatingAggregate(BigDecimal average,int count,Map<String,Integer> distribution){}
}
