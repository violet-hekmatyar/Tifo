package com.southstand.football.league.controller;

import com.southstand.common.result.Result;
import com.southstand.football.league.vo.LeagueVO;
import com.southstand.football.schedule.service.FootballQueryService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/app/football/leagues")
public class LeagueController {

    private final FootballQueryService footballQueryService;

    public LeagueController(FootballQueryService footballQueryService) {
        this.footballQueryService = footballQueryService;
    }

    @GetMapping
    public Result<List<LeagueVO>> leagues() {
        return Result.success(footballQueryService.leagues());
    }
}
