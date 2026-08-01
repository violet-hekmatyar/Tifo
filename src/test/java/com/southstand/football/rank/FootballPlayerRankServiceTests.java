package com.southstand.football.rank;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.southstand.common.exception.BusinessException;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
class FootballPlayerRankServiceTests {
 @Test void goalsRankingIsPagedAndUsesStablePosition(){var f=new FootballRankTestFixture();var stat=f.playerStat(1,40001,30001,12,5,new BigDecimal("7.81"),0);when(f.playerStats.selectCount(any(Wrapper.class))).thenReturn(41L);when(f.playerStats.selectList(any(Wrapper.class))).thenReturn(List.of(stat));when(f.players.selectBatchIds(any())).thenReturn(List.of(f.player(40001)));when(f.teams.selectBatchIds(any())).thenReturn(List.of(f.team(30001)));var page=f.service.playerRanks(FootballRankTestFixture.LEAGUE_ID,FootballRankTestFixture.SEASON_ID,null,"GOALS",2,20);assertThat(page.getTotal()).isEqualTo(41);assertThat(page.getRecords().get(0).rank()).isEqualTo(21);assertThat(page.getRecords().get(0).value()).isEqualByComparingTo("12");}
 @Test void supportedDecimalRanksAndInvalidType(){var f=new FootballRankTestFixture();assertThatThrownBy(()->f.service.playerRanks(FootballRankTestFixture.LEAGUE_ID,FootballRankTestFixture.SEASON_ID,null,"DROP TABLE",1,20)).isInstanceOf(BusinessException.class);assertThatThrownBy(()->f.service.playerRanks(FootballRankTestFixture.LEAGUE_ID,999L,null,"RATING",1,20)).isInstanceOf(BusinessException.class);}
}
