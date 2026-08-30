package com.southstand.football.player.controller;

import com.southstand.common.result.Result;
import com.southstand.football.detail.service.FootballDetailService;
import com.southstand.football.detail.vo.FootballDetailVO.Career;
import com.southstand.football.detail.vo.FootballDetailVO.PlayerOverview;
import com.southstand.football.detail.vo.FootballDetailVO.PlayerStats;
import com.southstand.football.detail.vo.FootballDetailVO.TeamHistory;
import com.southstand.football.detail.vo.FootballDetailVO.ContentSummary;
import com.southstand.football.detail.vo.FootballDetailVO.DetailMatch;
import com.southstand.common.result.PageResult;
import com.southstand.football.player.vo.PlayerDetailVO;
import com.southstand.football.schedule.service.FootballQueryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/app/football/players")
public class FootballPlayerController {

    private final FootballQueryService footballQueryService;
    private final FootballDetailService footballDetailService;

    public FootballPlayerController(FootballQueryService footballQueryService,FootballDetailService footballDetailService) {
        this.footballQueryService = footballQueryService;
        this.footballDetailService = footballDetailService;
    }

    @GetMapping("/{playerId}")
    public Result<PlayerDetailVO> playerDetail(@PathVariable Long playerId) {
        return Result.success(footballQueryService.playerDetail(playerId));
    }

    @GetMapping("/{playerId}/overview")
    public Result<PlayerOverview> overview(@PathVariable Long playerId,@RequestParam(required=false) Long seasonId){return Result.success(footballDetailService.playerOverview(playerId,seasonId));}

    @GetMapping("/{playerId}/stats")
    public Result<java.util.List<PlayerStats>> stats(@PathVariable Long playerId,@RequestParam(required=false) Long seasonId,@RequestParam(required=false) Long leagueId,@RequestParam(required=false) Long stageId){return Result.success(footballDetailService.playerStats(playerId,seasonId,leagueId,stageId));}

    @GetMapping("/{playerId}/teams")
    public Result<java.util.List<TeamHistory>> teams(@PathVariable Long playerId){return Result.success(footballDetailService.playerTeams(playerId));}

    @GetMapping("/{playerId}/career")
    public Result<Career> career(@PathVariable Long playerId){return Result.success(footballDetailService.playerCareer(playerId));}

    @GetMapping("/{playerId}/matches")
    public Result<PageResult<DetailMatch>> matches(@PathVariable Long playerId,
            @RequestParam(defaultValue="1") long pageNum,@RequestParam(defaultValue="20") long pageSize){
        return Result.success(footballDetailService.playerMatches(playerId,pageNum,pageSize));
    }

    @GetMapping("/{playerId}/contents")
    public Result<PageResult<ContentSummary>> contents(@PathVariable Long playerId,@RequestParam(required=false) String contentType,
            @RequestParam(defaultValue="1") long pageNum,@RequestParam(defaultValue="20") long pageSize){
        return Result.success(footballDetailService.playerContents(playerId,contentType,pageNum,pageSize));
    }
}
