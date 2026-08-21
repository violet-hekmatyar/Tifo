package com.southstand.football.matchdata;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.southstand.football.match.entity.MatchInfo;
import com.southstand.football.match.mapper.MatchInfoMapper;
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
import com.southstand.football.matchdata.service.MatchDataService;
import com.southstand.football.player.entity.FootballPlayer;
import com.southstand.football.player.mapper.FootballPlayerMapper;
import com.southstand.football.team.entity.FootballTeam;
import com.southstand.football.team.mapper.FootballTeamMapper;
import java.math.BigDecimal;
import java.util.List;

class MatchDataTestFixture {
    final long MATCH=50001L,HOME=30001L,AWAY=30002L,PLAYER=40001L,USER=10001L;
    final MatchInfoMapper matches=mock(MatchInfoMapper.class);final FootballTeamMapper teams=mock(FootballTeamMapper.class);final FootballPlayerMapper players=mock(FootballPlayerMapper.class);
    final FootballMatchLineupMapper lineups=mock(FootballMatchLineupMapper.class);final FootballMatchPlayerAppearanceMapper appearances=mock(FootballMatchPlayerAppearanceMapper.class);
    final FootballMatchTeamStatMapper teamStats=mock(FootballMatchTeamStatMapper.class);final FootballMatchPlayerStatMapper playerStats=mock(FootballMatchPlayerStatMapper.class);final FootballUserPlayerRatingMapper ratings=mock(FootballUserPlayerRatingMapper.class);
    final MatchDataService service;
    MatchDataTestFixture(){
        when(matches.selectById(MATCH)).thenReturn(match("FINISHED"));when(lineups.selectList(any(QueryWrapper.class))).thenReturn(List.of());when(appearances.selectList(any(QueryWrapper.class))).thenReturn(List.of());
        when(teamStats.selectList(any(QueryWrapper.class))).thenReturn(List.of());when(playerStats.selectList(any(QueryWrapper.class))).thenReturn(List.of());when(ratings.selectList(any(QueryWrapper.class))).thenReturn(List.of());
        when(teams.selectBatchIds(any())).thenReturn(List.of(team(HOME,"Home"),team(AWAY,"Away")));when(players.selectBatchIds(any())).thenReturn(List.of(player()));when(players.selectById(PLAYER)).thenReturn(player());when(appearances.selectCount(any(QueryWrapper.class))).thenReturn(1L);
        service=new MatchDataService(matches,teams,players,lineups,appearances,teamStats,playerStats,ratings);
    }
    MatchInfo match(String status){MatchInfo m=new MatchInfo();m.setId(MATCH);m.setLeagueId(10001L);m.setSeason("2025-2026");m.setHomeTeamId(HOME);m.setAwayTeamId(AWAY);m.setHomeScore(2);m.setAwayScore(1);m.setMatchStatus(status);m.setStatus("ACTIVE");m.setIsDeleted(0);return m;}
    FootballTeam team(long id,String name){FootballTeam t=new FootballTeam();t.setId(id);t.setTeamName(name);t.setLogoUrl("/"+id+".png");return t;}
    FootballPlayer player(){FootballPlayer p=new FootballPlayer();p.setId(PLAYER);p.setPlayerName("Player");p.setAvatarUrl("/p.png");return p;}
    FootballMatchLineup lineup(){FootballMatchLineup x=new FootballMatchLineup();x.setMatchId(MATCH);x.setTeamId(HOME);x.setFormation("4-3-3");return x;}
    FootballMatchPlayerAppearance appearance(String type,int started,int appeared){FootballMatchPlayerAppearance a=new FootballMatchPlayerAppearance();a.setMatchId(MATCH);a.setTeamId(HOME);a.setPlayerId(PLAYER);a.setLineupType(type);a.setPosition("FORWARD");a.setShirtNumber(9);a.setCaptainFlag(1);a.setStartedFlag(started);a.setAppearedFlag(appeared);a.setStatus("ACTIVE");a.setIsDeleted(0);return a;}
    FootballMatchPlayerStat playerStat(){FootballMatchPlayerStat s=new FootballMatchPlayerStat();s.setMatchId(MATCH);s.setTeamId(HOME);s.setPlayerId(PLAYER);s.setMinutes(90);s.setGoals(2);s.setAssists(0);s.setShots(3);s.setShotsOnTarget(2);s.setPasses(20);s.setSuccessfulPasses(15);s.setKeyPasses(1);s.setTackles(0);s.setInterceptions(0);s.setSaves(0);s.setYellowCards(0);s.setRedCards(0);s.setOfficialRating(new BigDecimal("8.00"));return s;}
    FootballMatchTeamStat teamStat(long team){FootballMatchTeamStat s=new FootballMatchTeamStat();s.setMatchId(MATCH);s.setTeamId(team);s.setPossession(new BigDecimal(team==HOME?"55.00":"45.00"));s.setShots(team==HOME?3:1);s.setShotsOnTarget(team==HOME?2:1);s.setCorners(2);s.setFouls(5);s.setOffsides(1);s.setYellowCards(0);s.setRedCards(0);s.setPasses(20);s.setSuccessfulPasses(15);s.setPassAccuracy(new BigDecimal("75.00"));s.setSaves(0);s.setExpectedGoals(new BigDecimal("1.50"));return s;}
    FootballUserPlayerRating rating(long user,String value){FootballUserPlayerRating r=new FootballUserPlayerRating();r.setMatchId(MATCH);r.setPlayerId(PLAYER);r.setUserId(user);r.setRating(new BigDecimal(value));r.setStatus("ACTIVE");r.setIsDeleted(0);return r;}
}
