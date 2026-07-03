package com.southstand.football.match.controller;

import com.southstand.common.result.PageResult;
import com.southstand.common.result.Result;
import com.southstand.football.match.vo.MatchDetailVO;
import com.southstand.football.match.vo.MatchListVO;
import com.southstand.football.schedule.service.FootballQueryService;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/app/football/matches")
public class FootballMatchController {

    private final FootballQueryService footballQueryService;

    public FootballMatchController(FootballQueryService footballQueryService) {
        this.footballQueryService = footballQueryService;
    }

    @GetMapping("/important")
    public Result<PageResult<MatchListVO>> importantMatches(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(defaultValue = "1") long pageNum,
            @RequestParam(defaultValue = "10") long pageSize
    ) {
        return Result.success(footballQueryService.importantMatches(date, pageNum, pageSize));
    }

    @GetMapping("/following-teams")
    public Result<PageResult<MatchListVO>> followingTeamMatches(
            @RequestParam(required = false) Long teamId,
            @RequestParam(defaultValue = "1") long pageNum,
            @RequestParam(defaultValue = "10") long pageSize
    ) {
        return Result.success(footballQueryService.followingTeamMatches(teamId, pageNum, pageSize));
    }

    @GetMapping
    public Result<PageResult<MatchListVO>> matches(
            @RequestParam(required = false) Long leagueId,
            @RequestParam(required = false) Long teamId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") long pageNum,
            @RequestParam(defaultValue = "10") long pageSize
    ) {
        return Result.success(footballQueryService.matches(leagueId, teamId, date, status, pageNum, pageSize));
    }

    @GetMapping("/{matchId}")
    public Result<MatchDetailVO> matchDetail(@PathVariable Long matchId) {
        return Result.success(footballQueryService.matchDetail(matchId));
    }
}
