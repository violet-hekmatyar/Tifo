package com.southstand.football.team.controller;

import com.southstand.common.result.Result;
import com.southstand.common.result.PageResult;
import com.southstand.football.detail.service.FootballDetailService;
import com.southstand.football.detail.vo.FootballDetailVO.Honor;
import com.southstand.football.detail.vo.FootballDetailVO.RosterPlayer;
import com.southstand.football.detail.vo.FootballDetailVO.TeamOverview;
import com.southstand.football.detail.vo.FootballDetailVO.TeamStats;
import com.southstand.football.schedule.service.FootballQueryService;
import com.southstand.football.team.vo.TeamDetailVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/app/football/teams")
public class FootballTeamController {

    private final FootballQueryService footballQueryService;
    private final FootballDetailService footballDetailService;

    public FootballTeamController(FootballQueryService footballQueryService,FootballDetailService footballDetailService) {
        this.footballQueryService = footballQueryService;
        this.footballDetailService = footballDetailService;
    }

    @GetMapping("/{teamId}")
    public Result<TeamDetailVO> teamDetail(@PathVariable Long teamId) {
        return Result.success(footballQueryService.teamDetail(teamId));
    }

    @GetMapping("/{teamId}/overview")
    public Result<TeamOverview> overview(@PathVariable Long teamId,@RequestParam(required=false) Long seasonId){return Result.success(footballDetailService.teamOverview(teamId,seasonId));}

    @GetMapping("/{teamId}/players")
    public Result<PageResult<RosterPlayer>> players(@PathVariable Long teamId,@RequestParam(required=false) Long seasonId,@RequestParam(required=false) String position,@RequestParam(required=false) String squadRole,@RequestParam(defaultValue="1") long pageNum,@RequestParam(defaultValue="50") long pageSize){return Result.success(footballDetailService.teamPlayers(teamId,seasonId,position,squadRole,pageNum,pageSize));}

    @GetMapping("/{teamId}/stats")
    public Result<TeamStats> stats(@PathVariable Long teamId,@RequestParam(required=false) Long seasonId,@RequestParam(required=false) Long stageId){return Result.success(footballDetailService.teamStats(teamId,seasonId,stageId));}

    @GetMapping("/{teamId}/honors")
    public Result<java.util.List<Honor>> honors(@PathVariable Long teamId,@RequestParam(required=false) String honorType){return Result.success(footballDetailService.teamHonors(teamId,honorType));}
}
