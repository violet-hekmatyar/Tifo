package com.southstand.football.matchdata;
import static org.assertj.core.api.Assertions.assertThat;import static org.mockito.ArgumentMatchers.any;import static org.mockito.Mockito.when;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;import java.util.List;import org.junit.jupiter.api.Test;
class MatchLineupServiceTests{
 @Test void emptyLineupReturnsHomeAndAwayShells(){var f=new MatchDataTestFixture();var v=f.service.lineups(f.MATCH);assertThat(v.home().teamId()).isEqualTo(f.HOME);assertThat(v.home().starters()).isEmpty();assertThat(v.away().teamId()).isEqualTo(f.AWAY);}
 @Test void starterAndCaptainAreMapped(){var f=new MatchDataTestFixture();when(f.lineups.selectList(any(QueryWrapper.class))).thenReturn(List.of(f.lineup()));when(f.appearances.selectList(any(QueryWrapper.class))).thenReturn(List.of(f.appearance("STARTER",1,1)));var v=f.service.lineups(f.MATCH);assertThat(v.home().formation()).isEqualTo("4-3-3");assertThat(v.home().starters()).singleElement().extracting("captain").isEqualTo(true);}
}
