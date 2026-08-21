package com.southstand.football.matchdata;
import static org.assertj.core.api.Assertions.assertThat;import static org.mockito.Mockito.mock;import static org.mockito.Mockito.verify;import static org.mockito.Mockito.when;
import com.southstand.football.match.controller.FootballMatchController;import com.southstand.football.match.vo.MatchDetailVO;import com.southstand.football.matchdata.service.MatchDataService;import com.southstand.football.matchdata.vo.MatchDataVO;import com.southstand.football.schedule.service.FootballQueryService;import java.util.List;import org.junit.jupiter.api.Test;
class MatchFootballControllerTests{
 @Test void detailIsEnhanced(){var q=mock(FootballQueryService.class);var d=mock(MatchDataService.class);var vo=new MatchDetailVO();vo.setMatchId(1L);when(q.matchDetail(1L)).thenReturn(vo);var result=new FootballMatchController(q,d).matchDetail(1L);verify(d).enhance(vo);assertThat(result.getData()).isSameAs(vo);}
 @Test void publicReadEndpointsDelegate(){var q=mock(FootballQueryService.class);var d=mock(MatchDataService.class);var value=new MatchDataVO.Lineups(null,null);when(d.lineups(1L)).thenReturn(value);assertThat(new FootballMatchController(q,d).lineups(1L).getData()).isSameAs(value);}
}
