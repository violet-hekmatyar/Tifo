package com.southstand.football.rank;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import com.southstand.common.enums.ErrorCode;
import com.southstand.common.exception.BusinessException;
import com.southstand.common.exception.GlobalExceptionHandler;
import com.southstand.football.rank.controller.FootballRankController;
import com.southstand.football.rank.service.FootballRankService;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
class FootballRankControllerTests {
 @Test void invalidRankTypeReturnsStableParameterError() throws Exception {var service=org.mockito.Mockito.mock(FootballRankService.class);when(service.playerRanks(any(),any(),any(),any(),any(Long.class),any(Long.class))).thenThrow(new BusinessException(ErrorCode.PARAM_ERROR,"invalid player rankType"));var mvc=MockMvcBuilders.standaloneSetup(new FootballRankController(service)).setControllerAdvice(new GlobalExceptionHandler()).build();mvc.perform(get("/api/app/football/player-ranks").param("leagueId","1").param("seasonId","2").param("rankType","DROP_TABLE")).andExpect(status().isOk()).andExpect(jsonPath("$.code").value(40001));}
}
