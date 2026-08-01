package com.southstand.football.rank;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.southstand.common.exception.BusinessException;
import java.util.List;
import org.junit.jupiter.api.Test;
class FootballStandingServiceTests {
 @Test void standingsKeepStoredStableRankAndFormula(){var f=new FootballRankTestFixture();var row=f.standing(30001L,1,12,4,4,40,20);when(f.standings.selectList(any(Wrapper.class))).thenReturn(List.of(row));when(f.teams.selectBatchIds(any())).thenReturn(List.of(f.team(30001L)));var table=f.service.standings(FootballRankTestFixture.LEAGUE_ID,FootballRankTestFixture.SEASON_ID,null,null);var vo=table.records().get(0);assertThat(vo.rank()).isEqualTo(1);assertThat(vo.played()).isEqualTo(vo.won()+vo.drawn()+vo.lost());assertThat(vo.goalDifference()).isEqualTo(vo.goalsFor()-vo.goalsAgainst());assertThat(vo.points()).isEqualTo(vo.won()*3+vo.drawn()-vo.deductionPoints());}
 @Test void leagueSeasonMismatchIsRejected(){var f=new FootballRankTestFixture();assertThatThrownBy(()->f.service.standings(999L,FootballRankTestFixture.SEASON_ID,null,null)).isInstanceOf(BusinessException.class);}
}
