package com.southstand.football.team.controller;

import com.southstand.common.result.Result;
import com.southstand.football.schedule.service.FootballQueryService;
import com.southstand.football.team.vo.TeamDetailVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/app/football/teams")
public class FootballTeamController {

    private final FootballQueryService footballQueryService;

    public FootballTeamController(FootballQueryService footballQueryService) {
        this.footballQueryService = footballQueryService;
    }

    @GetMapping("/{teamId}")
    public Result<TeamDetailVO> teamDetail(@PathVariable Long teamId) {
        return Result.success(footballQueryService.teamDetail(teamId));
    }
}
