package com.southstand.football.rank;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.LongStream;
import org.junit.jupiter.api.Test;
class FootballRankPerformanceStructureTests {
 @Test void playerRankUsesOnePageQueryAndTwoBatchEntityQueries(){var f=new FootballRankTestFixture();var stats=LongStream.rangeClosed(1,20).mapToObj(i->f.playerStat(i,40000+i,30000+i%4,20-(int)i%5,8,new BigDecimal("7.20"),0)).toList();when(f.playerStats.selectCount(any(Wrapper.class))).thenReturn(120L);when(f.playerStats.selectList(any(Wrapper.class))).thenReturn(stats);when(f.players.selectBatchIds(any())).thenReturn(List.of());when(f.teams.selectBatchIds(any())).thenReturn(List.of());var page=f.service.playerRanks(FootballRankTestFixture.LEAGUE_ID,FootballRankTestFixture.SEASON_ID,null,"GOALS",1,20);assertThat(page.getRecords()).hasSize(20);verify(f.playerStats,times(1)).selectCount(any(Wrapper.class));verify(f.playerStats,times(1)).selectList(any(Wrapper.class));verify(f.players,times(1)).selectBatchIds(any());verify(f.teams,times(1)).selectBatchIds(any());}
 @Test void standingLoadsTeamsInOneBatch(){var f=new FootballRankTestFixture();var rows=LongStream.rangeClosed(1,30).mapToObj(i->f.standing(30000+i,(int)i,10,3,7,40-(int)i,20)).toList();when(f.standings.selectList(any(Wrapper.class))).thenReturn(rows);when(f.teams.selectBatchIds(any())).thenReturn(List.of());assertThat(f.service.standings(FootballRankTestFixture.LEAGUE_ID,FootballRankTestFixture.SEASON_ID,null,null).records()).hasSize(30);verify(f.standings,times(1)).selectList(any(Wrapper.class));verify(f.teams,times(1)).selectBatchIds(any());}
}
