package com.southstand.card;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.southstand.card.model.HomeFeedUserContext;
import com.southstand.card.service.RankingCardService;
import com.southstand.card.vo.FeedCardVO;
import com.southstand.card.vo.RankingCardPayload;
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
import java.util.Collection;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class RankingCardServiceTests {

    @Test
    void mainTeamLeagueProducesThreeStronglyTypedRankingCards() {
        FootballLeagueMapper leagueMapper = mock(FootballLeagueMapper.class);
        MatchInfoMapper matchMapper = mock(MatchInfoMapper.class);
        FootballSeasonMapper seasonMapper = mock(FootballSeasonMapper.class);
        FootballCompetitionStageMapper stageMapper = mock(FootballCompetitionStageMapper.class);
        FootballStandingMapper standingMapper = mock(FootballStandingMapper.class);
        FootballPlayerCompetitionStatMapper playerStatMapper = mock(FootballPlayerCompetitionStatMapper.class);
        FootballTeamCompetitionStatMapper teamStatMapper = mock(FootballTeamCompetitionStatMapper.class);
        FootballTeamMapper teamMapper = mock(FootballTeamMapper.class);
        FootballPlayerMapper playerMapper = mock(FootballPlayerMapper.class);

        FootballLeague league = league(99L, "主队联赛");
        MatchInfo match = new MatchInfo(); match.setId(1L); match.setLeagueId(99L);
        match.setHomeTeamId(9L); match.setAwayTeamId(10L);
        when(matchMapper.selectList(any(Wrapper.class))).thenReturn(List.of(match));
        when(leagueMapper.selectById(99L)).thenReturn(league);
        FootballSeason season = new FootballSeason(); season.setId(2026L); season.setSeasonName("2026");
        when(seasonMapper.selectOne(any(Wrapper.class))).thenReturn(season);
        FootballCompetitionStage stage = new FootballCompetitionStage(); stage.setId(7L);
        when(stageMapper.selectOne(any(Wrapper.class))).thenReturn(stage);
        FootballStanding standing = new FootballStanding(); standing.setTeamId(9L); standing.setRankNo(1); standing.setPoints(80);
        when(standingMapper.selectList(any(Wrapper.class))).thenReturn(List.of(standing));
        FootballPlayerCompetitionStat playerStat = new FootballPlayerCompetitionStat();
        playerStat.setPlayerId(21L); playerStat.setTeamId(9L); playerStat.setGoals(25);
        when(playerStatMapper.selectList(any(Wrapper.class))).thenReturn(List.of(playerStat));
        FootballTeamCompetitionStat teamStat = new FootballTeamCompetitionStat(); teamStat.setTeamId(9L); teamStat.setGoalsFor(70);
        when(teamStatMapper.selectList(any(Wrapper.class))).thenReturn(List.of(teamStat));
        when(teamMapper.selectBatchIds(any(Collection.class))).thenReturn(List.of(team(9L, "主队")));
        FootballPlayer player = new FootballPlayer(); player.setId(21L); player.setPlayerName("射手");
        when(playerMapper.selectBatchIds(any(Collection.class))).thenReturn(List.of(player));

        RankingCardService service = new RankingCardService(leagueMapper, matchMapper, seasonMapper, stageMapper,
                standingMapper, playerStatMapper, teamStatMapper, teamMapper, playerMapper);
        List<FeedCardVO> cards = service.candidates(new HomeFeedUserContext(1L, 9L, Set.of(), Set.of(), Set.of()));

        assertThat(cards).hasSize(3).allMatch(card -> "RANKING".equals(card.getCardType()));
        assertThat(cards).extracting(FeedCardVO::getCardKey).containsExactly(
                "RANKING:STANDING:POINTS:99:2026", "RANKING:PLAYER:GOALS:99:2026", "RANKING:TEAM:GOALS_FOR:99:2026");
        assertThat(cards).extracting(card -> ((RankingCardPayload) card.getPayload()).rankingType())
                .containsExactly("STANDING", "PLAYER", "TEAM");
        assertThat(((RankingCardPayload) cards.get(1).getPayload()).items().get(0).name()).isEqualTo("射手");
    }

    @Test
    void noActiveSeasonReturnsNoRankingCards() {
        FootballLeagueMapper leagueMapper = mock(FootballLeagueMapper.class);
        when(leagueMapper.selectOne(any(Wrapper.class))).thenReturn(league(1L, "默认联赛"));
        RankingCardService service = new RankingCardService(leagueMapper, mock(MatchInfoMapper.class),
                mock(FootballSeasonMapper.class), mock(FootballCompetitionStageMapper.class),
                mock(FootballStandingMapper.class), mock(FootballPlayerCompetitionStatMapper.class),
                mock(FootballTeamCompetitionStatMapper.class), mock(FootballTeamMapper.class), mock(FootballPlayerMapper.class));

        assertThat(service.candidates(HomeFeedUserContext.anonymous())).isEmpty();
    }

    private FootballLeague league(Long id, String name) {
        FootballLeague league = new FootballLeague(); league.setId(id); league.setLeagueName(name);
        league.setStatus("ACTIVE"); league.setIsDeleted(0); return league;
    }

    private FootballTeam team(Long id, String name) {
        FootballTeam team = new FootballTeam(); team.setId(id); team.setTeamName(name); return team;
    }
}
