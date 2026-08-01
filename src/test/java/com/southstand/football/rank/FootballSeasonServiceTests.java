package com.southstand.football.rank;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.southstand.common.exception.BusinessException;
import java.util.List;
import org.junit.jupiter.api.Test;
class FootballSeasonServiceTests {
 @Test void seasonsExposeCurrentFirstAndRejectMissingLeague(){var f=new FootballRankTestFixture();when(f.seasons.selectList(any(Wrapper.class))).thenReturn(List.of(f.season(FootballRankTestFixture.SEASON_ID,true),f.season(20000L,false)));var rows=f.service.seasons(FootballRankTestFixture.LEAGUE_ID);assertThat(rows).extracting("seasonName").containsExactly("2025/26赛季","2024/25赛季");assertThat(rows.get(0).current()).isTrue();assertThatThrownBy(()->f.service.seasons(999L)).isInstanceOf(BusinessException.class);}
}
