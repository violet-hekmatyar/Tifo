package com.southstand.football.detail;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.southstand.common.result.PageResult;
import com.southstand.football.detail.service.MatchOverviewService;
import com.southstand.football.event.vo.MatchEventVO;
import com.southstand.football.league.entity.FootballLeague;
import com.southstand.football.league.mapper.FootballLeagueMapper;
import com.southstand.football.match.entity.MatchInfo;
import com.southstand.football.match.mapper.MatchInfoMapper;
import com.southstand.football.match.vo.MatchDetailVO;
import com.southstand.football.matchdata.mapper.FootballMatchPlayerAppearanceMapper;
import com.southstand.football.matchdata.mapper.FootballMatchPlayerStatMapper;
import com.southstand.football.matchdata.service.MatchDataService;
import com.southstand.football.matchdata.vo.MatchDataVO;
import com.southstand.football.rank.entity.FootballCompetitionStage;
import com.southstand.football.rank.entity.FootballSeason;
import com.southstand.football.rank.entity.FootballStanding;
import com.southstand.football.rank.mapper.FootballCompetitionStageMapper;
import com.southstand.football.rank.mapper.FootballSeasonMapper;
import com.southstand.football.rank.mapper.FootballStandingMapper;
import com.southstand.football.schedule.service.FootballQueryService;
import com.southstand.football.team.entity.FootballTeam;
import com.southstand.football.team.mapper.FootballTeamMapper;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import org.junit.jupiter.api.Test;

class MatchDetailServiceTests {

    @Test
    void overviewAggregatesCoreLineupStatsRatingsEventsAndBothStandings() {
        FootballQueryService query = mock(FootballQueryService.class); MatchDataService data = mock(MatchDataService.class);
        MatchInfoMapper matches = mock(MatchInfoMapper.class); FootballLeagueMapper leagues = mock(FootballLeagueMapper.class);
        FootballSeasonMapper seasons = mock(FootballSeasonMapper.class); FootballCompetitionStageMapper stages = mock(FootballCompetitionStageMapper.class);
        FootballStandingMapper standings = mock(FootballStandingMapper.class); FootballTeamMapper teams = mock(FootballTeamMapper.class);
        MatchDetailVO detail = new MatchDetailVO(); detail.setMatchId(500L); MatchEventVO event = new MatchEventVO(); event.setEventType("GOAL"); detail.setEventList(List.of(event));
        when(query.matchDetail(500L)).thenReturn(detail); when(data.lineups(500L)).thenReturn(new MatchDataVO.Lineups(null,null));
        when(data.teamStats(500L)).thenReturn(List.of()); when(data.playerStats(500L,null,null,1,100)).thenReturn(PageResult.of(List.of(),0,1,100));
        when(data.ratings(500L,null)).thenReturn(List.of()); when(matches.selectById(500L)).thenReturn(match());
        FootballLeague league = new FootballLeague(); league.setId(99L); league.setLeagueName("联赛"); when(leagues.selectById(99L)).thenReturn(league);
        FootballSeason season = new FootballSeason(); season.setId(88L); season.setSeasonName("2025-2026"); when(seasons.selectOne(any(Wrapper.class))).thenReturn(season);
        FootballCompetitionStage stage = new FootballCompetitionStage(); stage.setId(77L); stage.setStageName("联赛阶段"); when(stages.selectOne(any(Wrapper.class))).thenReturn(stage);
        when(standings.selectList(any(Wrapper.class))).thenReturn(List.of(standing(1L,1),standing(2L,2)));
        when(teams.selectBatchIds(any(Collection.class))).thenReturn(List.of(team(1L,"主队"),team(2L,"客队")));
        MatchOverviewService service = new MatchOverviewService(query,data,matches,leagues,seasons,stages,standings,teams);

        var overview = service.overview(500L);

        assertThat(overview.match().getEventList()).extracting("eventType").containsExactly("GOAL");
        assertThat(overview.lineups()).isNotNull(); assertThat(overview.teamStats()).isEmpty(); assertThat(overview.ratings()).isEmpty();
        assertThat(overview.ranking().snapshotType()).isEqualTo("CURRENT_STANDING");
        assertThat(overview.ranking().home().rank()).isEqualTo(1); assertThat(overview.ranking().away().rank()).isEqualTo(2);
        verify(standings,times(1)).selectList(any(Wrapper.class)); verify(teams,times(1)).selectBatchIds(any(Collection.class));
    }

    @Test
    void missingStandingKeepsStableNullSnapshots() {
        FootballQueryService query=mock(FootballQueryService.class);MatchDataService data=mock(MatchDataService.class);
        MatchInfoMapper matches=mock(MatchInfoMapper.class);FootballLeagueMapper leagues=mock(FootballLeagueMapper.class);
        FootballSeasonMapper seasons=mock(FootballSeasonMapper.class);FootballCompetitionStageMapper stages=mock(FootballCompetitionStageMapper.class);
        FootballStandingMapper standings=mock(FootballStandingMapper.class);FootballTeamMapper teams=mock(FootballTeamMapper.class);
        MatchDetailVO detail=new MatchDetailVO();detail.setMatchId(500L);detail.setEventList(List.of());when(query.matchDetail(500L)).thenReturn(detail);
        when(data.lineups(500L)).thenReturn(new MatchDataVO.Lineups(null,null));when(data.teamStats(500L)).thenReturn(List.of());
        when(data.playerStats(500L,null,null,1,100)).thenReturn(PageResult.of(List.of(),0,1,100));when(data.ratings(500L,null)).thenReturn(List.of());
        when(matches.selectById(500L)).thenReturn(match());FootballSeason season=new FootballSeason();season.setId(88L);when(seasons.selectOne(any(Wrapper.class))).thenReturn(season);
        when(standings.selectList(any(Wrapper.class))).thenReturn(List.of());when(teams.selectBatchIds(any(Collection.class))).thenReturn(List.of());
        var value=new MatchOverviewService(query,data,matches,leagues,seasons,stages,standings,teams).overview(500L);
        assertThat(value.ranking().home().rank()).isNull();assertThat(value.ranking().away().rank()).isNull();
    }

    private MatchInfo match(){MatchInfo m=new MatchInfo();m.setId(500L);m.setLeagueId(99L);m.setSeason("2025-2026");m.setHomeTeamId(1L);m.setAwayTeamId(2L);m.setMatchTime(LocalDateTime.now());m.setStatus("ACTIVE");m.setIsDeleted(0);return m;}
    private FootballStanding standing(long team,int rank){FootballStanding s=new FootballStanding();s.setTeamId(team);s.setRankNo(rank);s.setPlayed(20);s.setPoints(40-rank);return s;}
    private FootballTeam team(long id,String name){FootballTeam t=new FootballTeam();t.setId(id);t.setTeamName(name);return t;}
}
