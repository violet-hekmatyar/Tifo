package com.southstand.search;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.southstand.content.entity.Content;
import com.southstand.content.mapper.ContentMapper;
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
import com.southstand.search.service.SearchService;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class SearchServiceTests {

    @Test
    void searchesPublishedContentAndReturnsFlutterFields() {
        Fixture f = new Fixture();
        Content content = new Content();
        content.setId(40L);
        content.setTitle("欧冠焦点战");
        content.setSummary("赛前看点");
        content.setContentType("ARTICLE");
        content.setCoverUrl("/cover.jpg");
        content.setStatus("PUBLISHED");
        content.setPublishTime(LocalDateTime.of(2026, 8, 30, 12, 0));
        when(f.contents.selectList(any())).thenReturn(List.of(content));

        var page = f.service.entities("欧冠", "CONTENT", 1L, 20L);

        assertThat(page.getRecords()).singleElement().satisfies(vo -> {
            assertThat(vo.getEntityType()).isEqualTo("CONTENT");
            assertThat(vo.getEntityId()).isEqualTo(40L);
            assertThat(vo.getName()).isEqualTo("欧冠焦点战");
            assertThat(vo.getSubtitle()).isEqualTo("赛前看点");
            assertThat(vo.getContentType()).isEqualTo("ARTICLE");
            assertThat(vo.getPublishTime()).isEqualTo(content.getPublishTime());
        });
    }

    @Test
    void batchesPlayerTeamLookupInsteadOfSelectingPerPlayer() {
        Fixture f = new Fixture();
        FootballPlayer player = new FootballPlayer();
        player.setId(20L);
        player.setPlayerName("测试球员");
        TeamPlayer relation = new TeamPlayer();
        relation.setPlayerId(20L);
        relation.setTeamId(10L);
        FootballTeam team = team(10L, "测试球队");
        when(f.players.selectList(any())).thenReturn(List.of(player));
        when(f.teamPlayers.selectList(any())).thenReturn(List.of(relation));
        when(f.teams.selectBatchIds(any())).thenReturn(List.of(team));

        var page = f.service.entities("测试", "PLAYER", 1L, 20L);

        assertThat(page.getRecords()).singleElement().satisfies(vo -> assertThat(vo.getSubtitle()).contains("测试球队"));
        verify(f.teams, never()).selectById(any());
    }

    @Test
    void batchesMatchTeamAndLeagueLookup() {
        Fixture f = new Fixture();
        FootballTeam home = team(10L, "主队");
        FootballTeam away = team(11L, "客队");
        FootballLeague league = new FootballLeague();
        league.setId(30L);
        league.setLeagueName("测试联赛");
        MatchInfo match = new MatchInfo();
        match.setId(50L);
        match.setHomeTeamId(10L);
        match.setAwayTeamId(11L);
        match.setLeagueId(30L);
        when(f.teams.selectList(any())).thenReturn(List.of(home));
        when(f.leagues.selectList(any())).thenReturn(List.of());
        when(f.matches.selectList(any())).thenReturn(List.of(match));
        when(f.teams.selectBatchIds(any())).thenReturn(List.of(home, away));
        when(f.leagues.selectBatchIds(any())).thenReturn(List.of(league));

        var page = f.service.entities("主队", "MATCH", 1L, 20L);

        assertThat(page.getRecords()).singleElement().satisfies(vo -> {
            assertThat(vo.getName()).isEqualTo("主队 vs 客队");
            assertThat(vo.getSubtitle()).startsWith("测试联赛");
        });
        verify(f.teams, never()).selectById(any());
        verify(f.leagues, never()).selectById(any());
    }

    private static FootballTeam team(long id, String name) {
        FootballTeam team = new FootballTeam();
        team.setId(id);
        team.setTeamName(name);
        return team;
    }

    private static class Fixture {
        FootballTeamMapper teams = mock(FootballTeamMapper.class);
        FootballPlayerMapper players = mock(FootballPlayerMapper.class);
        TeamPlayerMapper teamPlayers = mock(TeamPlayerMapper.class);
        MatchInfoMapper matches = mock(MatchInfoMapper.class);
        FootballLeagueMapper leagues = mock(FootballLeagueMapper.class);
        ContentMapper contents = mock(ContentMapper.class);
        SearchService service = new SearchService(teams, players, teamPlayers, matches, leagues, contents);
    }
}
