package com.southstand.football.rank.controller;

import com.southstand.common.result.PageResult;
import com.southstand.common.result.Result;
import com.southstand.football.rank.service.FootballRankService;
import com.southstand.football.rank.vo.PlayerRankRecordVO;
import com.southstand.football.rank.vo.StandingTableVO;
import com.southstand.football.rank.vo.TeamRankRecordVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/app/football")
public class FootballRankController {
    private final FootballRankService service;
    public FootballRankController(FootballRankService service) { this.service = service; }

    @GetMapping("/standings")
    public Result<StandingTableVO> standings(@RequestParam Long leagueId, @RequestParam Long seasonId,
            @RequestParam(required=false) Long stageId, @RequestParam(required=false) String groupCode) {
        return Result.success(service.standings(leagueId, seasonId, stageId, groupCode));
    }

    @GetMapping("/player-ranks")
    public Result<PageResult<PlayerRankRecordVO>> playerRanks(@RequestParam Long leagueId, @RequestParam Long seasonId,
            @RequestParam(required=false) Long stageId, @RequestParam String rankType,
            @RequestParam(defaultValue="1") long pageNum, @RequestParam(defaultValue="20") long pageSize) {
        return Result.success(service.playerRanks(leagueId, seasonId, stageId, rankType, pageNum, pageSize));
    }

    @GetMapping("/team-ranks")
    public Result<PageResult<TeamRankRecordVO>> teamRanks(@RequestParam Long leagueId, @RequestParam Long seasonId,
            @RequestParam(required=false) Long stageId, @RequestParam String rankType,
            @RequestParam(defaultValue="1") long pageNum, @RequestParam(defaultValue="20") long pageSize) {
        return Result.success(service.teamRanks(leagueId, seasonId, stageId, rankType, pageNum, pageSize));
    }
}
