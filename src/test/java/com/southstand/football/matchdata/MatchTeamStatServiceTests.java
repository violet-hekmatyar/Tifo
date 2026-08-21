package com.southstand.football.matchdata;
import static org.assertj.core.api.Assertions.assertThat;import static org.mockito.ArgumentMatchers.any;import static org.mockito.Mockito.when;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;import java.util.List;import org.junit.jupiter.api.Test;
class MatchTeamStatServiceTests{@Test void returnsUnifiedHomeAwayItems(){var f=new MatchDataTestFixture();when(f.teamStats.selectList(any(QueryWrapper.class))).thenReturn(List.of(f.teamStat(f.HOME),f.teamStat(f.AWAY)));var v=f.service.teamStats(f.MATCH);assertThat(v).hasSize(12);assertThat(v.get(0).statType()).isEqualTo("POSSESSION");assertThat(v.get(0).homeValue()).isEqualTo(new java.math.BigDecimal("55.00"));}}
