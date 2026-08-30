package com.southstand.football.detail;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.southstand.football.match.entity.MatchInfo;
import com.southstand.football.matchdata.entity.FootballMatchPlayerAppearance;
import com.southstand.football.matchdata.entity.FootballMatchPlayerStat;
import com.southstand.football.player.entity.TeamPlayer;
import com.southstand.football.team.entity.FootballTeam;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import org.junit.jupiter.api.Test;

class PlayerDetailServiceTests {

    @Test
    void overviewIncludesStatsCareerClubNationalTeamAndBatchPlayerMatches() {
        FootballDetailTestFixture f = new FootballDetailTestFixture();
        TeamPlayer club = link(f.TEAM, "CLUB", 9); TeamPlayer national = link(33L, "NATIONAL_TEAM", 10);
        FootballTeam country = new FootballTeam(); country.setId(33L); country.setTeamName("测试国家队");
        when(f.teamPlayers.selectList(any(Wrapper.class))).thenReturn(List.of(club, national));
        when(f.teams.selectBatchIds(any(Collection.class))).thenReturn(List.of(f.team(), country));
        when(f.matchPlayerStats.selectList(any(Wrapper.class))).thenReturn(List.of(stat()));
        when(f.matches.selectBatchIds(any(Collection.class))).thenReturn(List.of(match()));
        when(f.matchAppearances.selectList(any(Wrapper.class))).thenReturn(List.of(appearance()));

        var overview = f.service.playerOverview(f.PLAYER, f.SEASON);

        assertThat(overview.playerStatus()).isEqualTo("ACTIVE");
        assertThat(overview.club().teamId()).isEqualTo(f.TEAM);
        assertThat(overview.nationalTeam().teamName()).isEqualTo("测试国家队");
        assertThat(overview.seasonStats()).isNotEmpty();
        assertThat(overview.career().totalGoals()).isEqualTo(10);
        assertThat(overview.recentMatches()).singleElement().satisfies(x -> {
            assertThat(x.starter()).isTrue(); assertThat(x.minutes()).isEqualTo(90);
            assertThat(x.goals()).isEqualTo(1); assertThat(x.officialRating()).isEqualByComparingTo("8.10");
        });
        verify(f.matches,times(1)).selectBatchIds(any(Collection.class));
        verify(f.matchAppearances,times(1)).selectList(any(Wrapper.class));
    }

    @Test
    void retiredPlayerKeepsContractButReturnsNoRecentMatches() {
        FootballDetailTestFixture f = new FootballDetailTestFixture(); var retired = f.player(); retired.setRetired(1);
        when(f.players.selectById(f.PLAYER)).thenReturn(retired);
        var overview = f.service.playerOverview(f.PLAYER, f.SEASON);
        assertThat(overview.retired()).isTrue();
        assertThat(overview.playerStatus()).isEqualTo("RETIRED");
        assertThat(overview.recentMatches()).isEmpty();
        assertThat(overview.nationalTeam()).isNull();
    }

    private TeamPlayer link(long teamId, String type, int shirt) {
        TeamPlayer x = new TeamPlayer(); x.setTeamId(teamId); x.setPlayerId(FootballDetailTestFixture.PLAYER);
        x.setTeamType(type); x.setShirtNumber(shirt); x.setStatus("ACTIVE"); x.setIsDeleted(0); return x;
    }
    private MatchInfo match() { MatchInfo x = new MatchInfo(); x.setId(91L); x.setLeagueId(FootballDetailTestFixture.LEAGUE);
        x.setHomeTeamId(FootballDetailTestFixture.TEAM); x.setAwayTeamId(33L); x.setHomeScore(1); x.setAwayScore(0);
        x.setMatchStatus("FINISHED"); x.setMatchTime(LocalDateTime.now()); x.setStatus("ACTIVE"); x.setIsDeleted(0); return x; }
    private FootballMatchPlayerStat stat() { FootballMatchPlayerStat x = new FootballMatchPlayerStat(); x.setMatchId(91L);
        x.setPlayerId(FootballDetailTestFixture.PLAYER); x.setTeamId(FootballDetailTestFixture.TEAM); x.setMinutes(90);
        x.setGoals(1); x.setAssists(0); x.setOfficialRating(new BigDecimal("8.10")); return x; }
    private FootballMatchPlayerAppearance appearance() { FootballMatchPlayerAppearance x = new FootballMatchPlayerAppearance();
        x.setMatchId(91L); x.setPlayerId(FootballDetailTestFixture.PLAYER); x.setTeamId(FootballDetailTestFixture.TEAM);
        x.setStartedFlag(1); x.setStatus("ACTIVE"); x.setIsDeleted(0); return x; }
}
