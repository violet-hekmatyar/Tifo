package com.southstand.football.league.controller;

import com.southstand.common.result.Result;
import com.southstand.football.league.vo.LeagueVO;
import com.southstand.football.schedule.service.FootballQueryService;
import com.southstand.football.rank.service.FootballRankService;
import com.southstand.football.rank.vo.SeasonVO;
import com.southstand.football.rank.vo.StageVO;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/app/football/leagues")
public class LeagueController {

    private final FootballQueryService footballQueryService;
    private final FootballRankService footballRankService;

    public LeagueController(FootballQueryService footballQueryService, FootballRankService footballRankService) {
        this.footballQueryService = footballQueryService;
        this.footballRankService = footballRankService;
    }

    @GetMapping
    public Result<List<LeagueVO>> leagues() {
        return Result.success(footballQueryService.leagues());
    }

    @GetMapping("/{leagueId}/seasons")
    public Result<List<SeasonVO>> seasons(@PathVariable Long leagueId) {
        return Result.success(footballRankService.seasons(leagueId));
    }

    @GetMapping("/{leagueId}/seasons/{seasonId}/stages")
    public Result<List<StageVO>> stages(@PathVariable Long leagueId, @PathVariable Long seasonId) {
        return Result.success(footballRankService.stages(leagueId, seasonId));
    }
}
