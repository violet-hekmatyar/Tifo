package com.southstand.card;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.southstand.card.model.HomeFeedUserContext;
import com.southstand.card.service.PlayerRatingCardService;
import com.southstand.card.vo.FeedCardVO;
import com.southstand.card.vo.PlayerRatingCardPayload;
import com.southstand.football.league.entity.FootballLeague;
import com.southstand.football.league.mapper.FootballLeagueMapper;
import com.southstand.football.match.entity.MatchInfo;
import com.southstand.football.match.mapper.MatchInfoMapper;
import com.southstand.football.matchdata.entity.FootballMatchPlayerStat;
import com.southstand.football.matchdata.entity.FootballUserPlayerRating;
import com.southstand.football.matchdata.mapper.FootballMatchPlayerStatMapper;
import com.southstand.football.matchdata.mapper.FootballUserPlayerRatingMapper;
import com.southstand.football.player.entity.FootballPlayer;
import com.southstand.football.player.mapper.FootballPlayerMapper;
import com.southstand.football.team.entity.FootballTeam;
import com.southstand.football.team.mapper.FootballTeamMapper;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class PlayerRatingCardServiceTests {

    @Test
    void mainTeamMatchUsesRealUserAverageAndOfficialFallback() {
        MatchInfoMapper matchMapper = mock(MatchInfoMapper.class);
        FootballMatchPlayerStatMapper statMapper = mock(FootballMatchPlayerStatMapper.class);
        FootballUserPlayerRatingMapper ratingMapper = mock(FootballUserPlayerRatingMapper.class);
        FootballLeagueMapper leagueMapper = mock(FootballLeagueMapper.class);
        FootballTeamMapper teamMapper = mock(FootballTeamMapper.class);
        FootballPlayerMapper playerMapper = mock(FootballPlayerMapper.class);
        MatchInfo match = match();
        when(matchMapper.selectList(any(Wrapper.class))).thenReturn(List.of(match));
        when(statMapper.selectList(any(Wrapper.class))).thenReturn(List.of(stat(1L, 11L, 9L, "8.20"), stat(2L, 12L, 10L, "7.50")));
        when(ratingMapper.selectList(any(Wrapper.class))).thenReturn(List.of(rating(1L, 11L, 101L, "9.00"), rating(2L, 11L, 102L, "8.00")));
        when(playerMapper.selectBatchIds(any(Collection.class))).thenReturn(List.of(player(11L, "甲"), player(12L, "乙")));
        when(teamMapper.selectBatchIds(any(Collection.class))).thenReturn(List.of(team(9L, "主队"), team(10L, "客队")));
        FootballLeague league = new FootballLeague(); league.setId(99L); league.setLeagueName("测试联赛");
        when(leagueMapper.selectById(99L)).thenReturn(league);
        PlayerRatingCardService service = new PlayerRatingCardService(matchMapper, statMapper, ratingMapper,
                leagueMapper, teamMapper, playerMapper);

        List<FeedCardVO> cards = service.candidates(new HomeFeedUserContext(1L, 9L, Set.of(), Set.of(), Set.of()));

        assertThat(cards).singleElement().satisfies(card -> {
            assertThat(card.getCardKey()).isEqualTo("PLAYER_RATING:500");
            assertThat(card.getReasonCode()).isEqualTo("MAIN_TEAM");
            PlayerRatingCardPayload payload = (PlayerRatingCardPayload) card.getPayload();
            assertThat(payload.ratingUserCount()).isEqualTo(2);
            assertThat(payload.topPlayers().get(0).userRatingAverage()).isEqualByComparingTo("8.50");
            assertThat(payload.topPlayers().get(0).userRatingCount()).isEqualTo(2);
            assertThat(payload.topPlayers().get(1).userRatingAverage()).isNull();
            assertThat(payload.topPlayers().get(1).officialRating()).isEqualByComparingTo("7.50");
        });
    }

    @Test
    void matchWithoutPlayerStatsReturnsNoCard() {
        MatchInfoMapper matchMapper = mock(MatchInfoMapper.class);
        when(matchMapper.selectList(any(Wrapper.class))).thenReturn(List.of(match()));
        PlayerRatingCardService service = new PlayerRatingCardService(matchMapper,
                mock(FootballMatchPlayerStatMapper.class), mock(FootballUserPlayerRatingMapper.class),
                mock(FootballLeagueMapper.class), mock(FootballTeamMapper.class), mock(FootballPlayerMapper.class));

        assertThat(service.candidates(HomeFeedUserContext.anonymous())).isEmpty();
    }

    private MatchInfo match() {
        MatchInfo match = new MatchInfo(); match.setId(500L); match.setLeagueId(99L);
        match.setHomeTeamId(9L); match.setAwayTeamId(10L); match.setHomeScore(2); match.setAwayScore(1);
        match.setImportantLevel(5); match.setMatchStatus("FINISHED"); match.setMatchTime(LocalDateTime.now()); return match;
    }

    private FootballMatchPlayerStat stat(Long id, Long playerId, Long teamId, String officialRating) {
        FootballMatchPlayerStat stat = new FootballMatchPlayerStat(); stat.setId(id); stat.setMatchId(500L);
        stat.setPlayerId(playerId); stat.setTeamId(teamId); stat.setOfficialRating(new BigDecimal(officialRating)); return stat;
    }

    private FootballUserPlayerRating rating(Long id, Long playerId, Long userId, String score) {
        FootballUserPlayerRating rating = new FootballUserPlayerRating(); rating.setId(id); rating.setMatchId(500L);
        rating.setPlayerId(playerId); rating.setUserId(userId); rating.setRating(new BigDecimal(score)); return rating;
    }

    private FootballPlayer player(Long id, String name) {
        FootballPlayer player = new FootballPlayer(); player.setId(id); player.setPlayerName(name); return player;
    }

    private FootballTeam team(Long id, String name) {
        FootballTeam team = new FootballTeam(); team.setId(id); team.setTeamName(name); return team;
    }
}
