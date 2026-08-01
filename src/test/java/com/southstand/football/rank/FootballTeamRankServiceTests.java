package com.southstand.football.rank;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import java.util.List;
import org.junit.jupiter.api.Test;
class FootballTeamRankServiceTests {
 @Test void goalsAgainstUsesAscendingDirectionAndBackendRank(){var f=new FootballRankTestFixture();var stat=f.teamStat(1,30001,42,14);when(f.teamStats.selectCount(any(Wrapper.class))).thenReturn(1L);when(f.teamStats.selectList(any(Wrapper.class))).thenReturn(List.of(stat));when(f.teams.selectBatchIds(any())).thenReturn(List.of(f.team(30001)));var row=f.service.teamRanks(FootballRankTestFixture.LEAGUE_ID,FootballRankTestFixture.SEASON_ID,null,"GOALS_AGAINST",1,20).getRecords().get(0);assertThat(row.sortDirection()).isEqualTo("ASC");assertThat(row.rank()).isEqualTo(1);assertThat(row.value()).isEqualByComparingTo("14");}
}
