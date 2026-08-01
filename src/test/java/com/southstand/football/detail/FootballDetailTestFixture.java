package com.southstand.football.detail;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.southstand.content.mapper.ContentMapper;
import com.southstand.content.mapper.ContentRelationMapper;
import com.southstand.follow.mapper.FollowRecordMapper;
import com.southstand.football.detail.entity.*;
import com.southstand.football.detail.mapper.*;
import com.southstand.football.detail.service.FootballDetailService;
import com.southstand.football.league.entity.FootballLeague;
import com.southstand.football.league.mapper.FootballLeagueMapper;
import com.southstand.football.match.mapper.MatchInfoMapper;
import com.southstand.football.player.entity.FootballPlayer;
import com.southstand.football.player.mapper.FootballPlayerMapper;
import com.southstand.football.rank.entity.*;
import com.southstand.football.rank.mapper.*;
import com.southstand.football.team.entity.FootballTeam;
import com.southstand.football.team.mapper.FootballTeamMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

class FootballDetailTestFixture {
 static final long TEAM=31,PLAYER=41,LEAGUE=51,SEASON=61,STAGE=71;
 final FootballTeamMapper teams=mock(FootballTeamMapper.class); final FootballPlayerMapper players=mock(FootballPlayerMapper.class);
 final FootballLeagueMapper leagues=mock(FootballLeagueMapper.class); final FootballSeasonMapper seasons=mock(FootballSeasonMapper.class);
 final FootballTeamSeasonPlayerMapper rosters=mock(FootballTeamSeasonPlayerMapper.class); final FootballTeamHonorMapper honors=mock(FootballTeamHonorMapper.class);
 final FootballPlayerTeamHistoryMapper histories=mock(FootballPlayerTeamHistoryMapper.class); final FootballPlayerCompetitionStatMapper playerStats=mock(FootballPlayerCompetitionStatMapper.class);
 final FootballTeamCompetitionStatMapper teamStats=mock(FootballTeamCompetitionStatMapper.class); final FootballStandingMapper standings=mock(FootballStandingMapper.class);
 final MatchInfoMapper matches=mock(MatchInfoMapper.class); final ContentRelationMapper relations=mock(ContentRelationMapper.class); final ContentMapper contents=mock(ContentMapper.class); final FollowRecordMapper follows=mock(FollowRecordMapper.class);
 final FootballDetailService service;
 FootballDetailTestFixture(){
  when(teams.selectById(TEAM)).thenReturn(team());when(players.selectById(PLAYER)).thenReturn(player());when(leagues.selectById(LEAGUE)).thenReturn(league());when(seasons.selectById(SEASON)).thenReturn(season());
  when(rosters.selectList(any(Wrapper.class))).thenReturn(List.of(roster()));when(rosters.selectOne(any(Wrapper.class))).thenReturn(roster());
  when(playerStats.selectList(any(Wrapper.class))).thenReturn(List.of(playerStat()));when(teamStats.selectOne(any(Wrapper.class))).thenReturn(teamStat());when(standings.selectOne(any(Wrapper.class))).thenReturn(standing());
  when(histories.selectList(any(Wrapper.class))).thenReturn(List.of(history()));when(honors.selectList(any(Wrapper.class))).thenReturn(List.of(honor()));
  when(matches.selectList(any(Wrapper.class))).thenReturn(List.of());when(relations.selectList(any(Wrapper.class))).thenReturn(List.of());when(follows.selectList(any(Wrapper.class))).thenReturn(List.of());
  when(teams.selectBatchIds(any(Collection.class))).thenReturn(List.of(team()));when(players.selectBatchIds(any(Collection.class))).thenReturn(List.of(player()));when(leagues.selectBatchIds(any(Collection.class))).thenReturn(List.of(league()));when(seasons.selectBatchIds(any(Collection.class))).thenReturn(List.of(season()));
  service=new FootballDetailService(teams,players,leagues,seasons,rosters,honors,histories,playerStats,teamStats,standings,matches,relations,contents,follows);
 }
 FootballTeam team(){var x=new FootballTeam();x.setId(TEAM);x.setTeamName("测试球队");x.setStatus("ACTIVE");x.setIsDeleted(0);return x;}
 FootballPlayer player(){var x=new FootballPlayer();x.setId(PLAYER);x.setPlayerName("测试球员");x.setBirthDate(LocalDate.of(2000,1,1));x.setStatus("ACTIVE");x.setIsDeleted(0);return x;}
 FootballLeague league(){var x=new FootballLeague();x.setId(LEAGUE);x.setLeagueName("测试联赛");return x;}
 FootballSeason season(){var x=new FootballSeason();x.setId(SEASON);x.setLeagueId(LEAGUE);x.setSeasonName("2025/26赛季");x.setStartDate(LocalDate.of(2025,8,1));x.setCurrentFlag(1);return x;}
 FootballTeamSeasonPlayer roster(){var x=new FootballTeamSeasonPlayer();x.setId(1L);x.setLeagueId(LEAGUE);x.setSeasonId(SEASON);x.setTeamId(TEAM);x.setPlayerId(PLAYER);x.setPosition("FORWARD");x.setShirtNumber(9);x.setSquadRole("FIRST_TEAM");x.setCaptainFlag(1);x.setLoanFlag(0);x.setStatus("ACTIVE");x.setIsDeleted(0);return x;}
 FootballPlayerCompetitionStat playerStat(){var x=new FootballPlayerCompetitionStat();x.setId(2L);x.setLeagueId(LEAGUE);x.setSeasonId(SEASON);x.setStageId(STAGE);x.setPlayerId(PLAYER);x.setTeamId(TEAM);x.setAppearances(20);x.setStarts(18);x.setMinutes(1600);x.setGoals(10);x.setAssists(5);x.setYellowCards(2);x.setRedCards(0);x.setShots(40);x.setShotsOnTarget(20);x.setRating(new BigDecimal("7.50"));x.setSaves(0);x.setSource("DEMO");x.setIsDeleted(0);return x;}
 FootballTeamCompetitionStat teamStat(){var x=new FootballTeamCompetitionStat();x.setTeamId(TEAM);x.setSeasonId(SEASON);x.setPlayed(20);x.setGoalsFor(40);x.setGoalsAgainst(20);x.setAssists(30);x.setShots(200);x.setShotsOnTarget(100);x.setCorners(80);x.setFouls(150);x.setYellowCards(20);x.setRedCards(1);x.setCleanSheets(8);x.setAvgRating(new BigDecimal("7.20"));x.setSource("DEMO");return x;}
 FootballStanding standing(){var x=new FootballStanding();x.setTeamId(TEAM);x.setSeasonId(SEASON);x.setRankNo(1);x.setPlayed(20);x.setWon(12);x.setDrawn(4);x.setLost(4);x.setGoalsFor(40);x.setGoalsAgainst(20);x.setGoalDifference(20);x.setPoints(40);return x;}
 FootballPlayerTeamHistory history(){var x=new FootballPlayerTeamHistory();x.setPlayerId(PLAYER);x.setTeamId(TEAM);x.setSeasonId(SEASON);x.setStartDate(LocalDate.of(2025,8,1));x.setShirtNumber(9);x.setPosition("FORWARD");x.setAppearances(20);x.setGoals(10);x.setAssists(5);x.setCurrentFlag(1);x.setLoanFlag(0);x.setIsDeleted(0);return x;}
 FootballTeamHonor honor(){var x=new FootballTeamHonor();x.setId(3L);x.setTeamId(TEAM);x.setHonorName("Demo 杯");x.setHonorType("OTHER");x.setTitleCount(1);x.setWinningYears("2025");x.setLatestYear(2025);x.setIsDeleted(0);return x;}
}
