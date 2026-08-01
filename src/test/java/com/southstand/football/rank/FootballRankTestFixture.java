package com.southstand.football.rank;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.southstand.football.league.entity.FootballLeague;
import com.southstand.football.league.mapper.FootballLeagueMapper;
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
import com.southstand.football.rank.service.FootballRankService;
import com.southstand.football.team.entity.FootballTeam;
import com.southstand.football.team.mapper.FootballTeamMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

class FootballRankTestFixture {
    static final long LEAGUE_ID=12001L, SEASON_ID=20001L, STAGE_ID=21001L;
    final FootballLeagueMapper leagues=mock(FootballLeagueMapper.class);
    final FootballSeasonMapper seasons=mock(FootballSeasonMapper.class);
    final FootballCompetitionStageMapper stages=mock(FootballCompetitionStageMapper.class);
    final FootballStandingMapper standings=mock(FootballStandingMapper.class);
    final FootballPlayerCompetitionStatMapper playerStats=mock(FootballPlayerCompetitionStatMapper.class);
    final FootballTeamCompetitionStatMapper teamStats=mock(FootballTeamCompetitionStatMapper.class);
    final FootballPlayerMapper players=mock(FootballPlayerMapper.class);
    final FootballTeamMapper teams=mock(FootballTeamMapper.class);
    final FootballRankService service;

    FootballRankTestFixture() {
        when(leagues.selectById(LEAGUE_ID)).thenReturn(league());
        when(seasons.selectById(SEASON_ID)).thenReturn(season(SEASON_ID, true));
        when(stages.selectById(STAGE_ID)).thenReturn(stage());
        when(stages.selectOne(any(Wrapper.class))).thenReturn(stage());
        service=new FootballRankService(leagues,seasons,stages,standings,playerStats,teamStats,players,teams);
    }
    FootballLeague league(){ FootballLeague x=new FootballLeague(); x.setId(LEAGUE_ID);x.setLeagueName("英格兰超级联赛");x.setStatus("ACTIVE");x.setIsDeleted(0);return x; }
    FootballSeason season(long id,boolean current){FootballSeason x=new FootballSeason();x.setId(id);x.setLeagueId(LEAGUE_ID);x.setSeasonCode(current?"2025-2026":"2024-2025");x.setSeasonName(current?"2025/26赛季":"2024/25赛季");x.setStartDate(LocalDate.of(current?2025:2024,8,1));x.setEndDate(LocalDate.of(current?2026:2025,6,30));x.setCurrentFlag(current?1:0);x.setStatus("ACTIVE");x.setSource("DEMO");x.setIsDeleted(0);return x;}
    FootballCompetitionStage stage(){FootballCompetitionStage x=new FootballCompetitionStage();x.setId(STAGE_ID);x.setLeagueId(LEAGUE_ID);x.setSeasonId(SEASON_ID);x.setStageType("LEAGUE");x.setStageName("联赛阶段");x.setSortOrder(1);x.setStatus("ACTIVE");x.setIsDeleted(0);return x;}
    FootballTeam team(long id){FootballTeam x=new FootballTeam();x.setId(id);x.setTeamName("球队"+id);x.setLogoUrl("/team.svg");x.setStatus("ACTIVE");x.setIsDeleted(0);return x;}
    FootballPlayer player(long id){FootballPlayer x=new FootballPlayer();x.setId(id);x.setPlayerName("球员"+id);x.setAvatarUrl("/player.svg");x.setStatus("ACTIVE");x.setIsDeleted(0);return x;}
    FootballStanding standing(long teamId,int rank,int won,int drawn,int lost,int gf,int ga){FootballStanding x=new FootballStanding();x.setId(22000L+rank);x.setLeagueId(LEAGUE_ID);x.setSeasonId(SEASON_ID);x.setStageId(STAGE_ID);x.setGroupCode("");x.setTeamId(teamId);x.setRankNo(rank);x.setPlayed(won+drawn+lost);x.setWon(won);x.setDrawn(drawn);x.setLost(lost);x.setGoalsFor(gf);x.setGoalsAgainst(ga);x.setGoalDifference(gf-ga);x.setDeductionPoints(0);x.setPoints(won*3+drawn);x.setFormText("胜平胜");x.setSource("DEMO");x.setSourceUpdatedAt(LocalDateTime.of(2026,7,20,12,0));return x;}
    FootballPlayerCompetitionStat playerStat(long id,long playerId,long teamId,int goals,int assists,BigDecimal rating,int saves){FootballPlayerCompetitionStat x=new FootballPlayerCompetitionStat();x.setId(id);x.setLeagueId(LEAGUE_ID);x.setSeasonId(SEASON_ID);x.setStageId(STAGE_ID);x.setPlayerId(playerId);x.setTeamId(teamId);x.setAppearances(20);x.setStarts(18);x.setMinutes(1600);x.setGoals(goals);x.setAssists(assists);x.setYellowCards(2);x.setRedCards(0);x.setShots(40);x.setShotsOnTarget(20);x.setRating(rating);x.setSaves(saves);x.setSourceUpdatedAt(LocalDateTime.of(2026,7,20,12,0));return x;}
    FootballTeamCompetitionStat teamStat(long id,long teamId,int gf,int ga){FootballTeamCompetitionStat x=new FootballTeamCompetitionStat();x.setId(id);x.setLeagueId(LEAGUE_ID);x.setSeasonId(SEASON_ID);x.setStageId(STAGE_ID);x.setTeamId(teamId);x.setPlayed(20);x.setGoalsFor(gf);x.setGoalsAgainst(ga);x.setAssists(gf-2);x.setYellowCards(20);x.setRedCards(1);x.setShots(200);x.setShotsOnTarget(90);x.setCorners(100);x.setFouls(180);x.setCleanSheets(8);x.setAvgRating(new BigDecimal("7.20"));x.setSourceUpdatedAt(LocalDateTime.of(2026,7,20,12,0));return x;}
}
