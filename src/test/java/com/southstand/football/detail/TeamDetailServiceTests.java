package com.southstand.football.detail;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.southstand.content.entity.Content;
import com.southstand.content.entity.ContentRelation;
import com.southstand.football.match.entity.MatchInfo;
import com.southstand.football.team.entity.FootballTeam;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import org.junit.jupiter.api.Test;

class TeamDetailServiceTests {

    @Test
    void overviewContainsBasicRosterDataRankingsLeaderboardsPostsAndHonors() {
        FootballDetailTestFixture f = new FootballDetailTestFixture();
        when(f.standings.selectList(any(Wrapper.class))).thenReturn(List.of(f.standing()));
        ContentRelation relation = new ContentRelation(); relation.setContentId(81L);
        when(f.relations.selectList(any(Wrapper.class))).thenReturn(List.of(relation));
        when(f.contents.selectList(any(Wrapper.class))).thenReturn(List.of(content()));

        var overview = f.service.teamOverview(f.TEAM, f.SEASON);

        assertThat(overview.teamName()).isEqualTo("测试球队");
        assertThat(overview.topScorers()).singleElement().satisfies(x -> assertThat(x.goals()).isEqualTo(10));
        assertThat(overview.competitionStandings()).singleElement().satisfies(x -> assertThat(x.rank()).isEqualTo(1));
        assertThat(overview.leaderboards()).extracting("rankType")
                .containsExactly("GOALS", "ASSISTS", "APPEARANCES", "RATING");
        assertThat(overview.recentContents()).singleElement().satisfies(x -> assertThat(x.contentType()).isEqualTo("ARTICLE"));
        assertThat(overview.honors()).singleElement().satisfies(x -> assertThat(x.honorName()).isEqualTo("Demo 杯"));
    }

    @Test
    void teamMatchesArePagedAndTeamsAndLeaguesAreBatchLoaded() {
        FootballDetailTestFixture f = new FootballDetailTestFixture(); MatchInfo match = match();
        FootballTeam away = new FootballTeam(); away.setId(32L); away.setTeamName("客队");
        when(f.matches.selectList(any(Wrapper.class))).thenReturn(List.of(match));
        when(f.teams.selectBatchIds(any(Collection.class))).thenReturn(List.of(f.team(), away));

        var page = f.service.teamMatches(f.TEAM, "RECENT", 1, 20);

        assertThat(page.getRecords()).singleElement().satisfies(x -> {
            assertThat(x.matchStatus()).isEqualTo("FINISHED");
            assertThat(x.homeTeamName()).isEqualTo("测试球队");
            assertThat(x.awayTeamName()).isEqualTo("客队");
            assertThat(x.playerTeamId()).isEqualTo(f.TEAM);
        });
        verify(f.teams,times(1)).selectBatchIds(any(Collection.class));
        verify(f.leagues,times(1)).selectBatchIds(any(Collection.class));
    }

    @Test
    void teamContentsReturnEmptyArrayWhenNoRelationsExist() {
        FootballDetailTestFixture f = new FootballDetailTestFixture();
        assertThat(f.service.teamContents(f.TEAM, null, 1, 20).getRecords()).isEmpty();
    }

    private Content content() {
        Content c = new Content(); c.setId(81L); c.setContentType("ARTICLE"); c.setTitle("球队文章");
        c.setHotScore(BigDecimal.TEN); c.setPublishTime(LocalDateTime.now()); c.setLikeCount(3);
        c.setCommentCount(2); c.setFavoriteCount(1); return c;
    }

    private MatchInfo match() {
        MatchInfo m = new MatchInfo(); m.setId(91L); m.setLeagueId(FootballDetailTestFixture.LEAGUE);
        m.setHomeTeamId(FootballDetailTestFixture.TEAM); m.setAwayTeamId(32L); m.setHomeScore(2); m.setAwayScore(1);
        m.setMatchStatus("FINISHED"); m.setMatchTime(LocalDateTime.now()); m.setStatus("ACTIVE"); m.setIsDeleted(0); return m;
    }
}
