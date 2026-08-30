package com.southstand.football.match.controller;

import com.southstand.common.result.PageResult;
import com.southstand.common.result.Result;
import com.southstand.football.match.vo.MatchDetailVO;
import com.southstand.football.match.vo.MatchListVO;
import com.southstand.football.schedule.service.FootballQueryService;
import com.southstand.football.matchdata.dto.PlayerRatingRequest;
import com.southstand.football.matchdata.service.MatchDataService;
import com.southstand.football.matchdata.vo.MatchDataVO;
import com.southstand.football.detail.service.MatchOverviewService;
import com.southstand.football.detail.vo.MatchOverviewVO;
import jakarta.validation.Valid;
import java.util.List;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.beans.factory.annotation.Autowired;

@RestController
@RequestMapping("/api/app/football/matches")
public class FootballMatchController {

    private final FootballQueryService footballQueryService;
    private final MatchDataService matchDataService;
    private final MatchOverviewService matchOverviewService;

    @Autowired
    public FootballMatchController(FootballQueryService footballQueryService,MatchDataService matchDataService,
                                   MatchOverviewService matchOverviewService) {
        this.footballQueryService = footballQueryService;
        this.matchDataService = matchDataService;
        this.matchOverviewService = matchOverviewService;
    }

    public FootballMatchController(FootballQueryService footballQueryService,MatchDataService matchDataService) {
        this.footballQueryService = footballQueryService;
        this.matchDataService = matchDataService;
        this.matchOverviewService = null;
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
        MatchDetailVO detail=footballQueryService.matchDetail(matchId);
        matchDataService.enhance(detail);
        return Result.success(detail);
    }

    @GetMapping("/{matchId}/lineups")
    public Result<MatchDataVO.Lineups> lineups(@PathVariable Long matchId){return Result.success(matchDataService.lineups(matchId));}

    @GetMapping("/{matchId}/stats")
    public Result<List<MatchDataVO.TeamStatItem>> stats(@PathVariable Long matchId){return Result.success(matchDataService.teamStats(matchId));}

    @GetMapping("/{matchId}/player-stats")
    public Result<PageResult<MatchDataVO.PlayerStat>> playerStats(@PathVariable Long matchId,@RequestParam(required=false)Long teamId,
            @RequestParam(required=false)String position,@RequestParam(defaultValue="1")long pageNum,@RequestParam(defaultValue="50")long pageSize){
        return Result.success(matchDataService.playerStats(matchId,teamId,position,pageNum,pageSize));
    }

    @GetMapping("/{matchId}/ratings")
    public Result<List<MatchDataVO.RatingSummary>> ratings(@PathVariable Long matchId,@RequestParam(required=false)Long teamId){return Result.success(matchDataService.ratings(matchId,teamId));}

    @GetMapping("/{matchId}/overview")
    public Result<MatchOverviewVO> overview(@PathVariable Long matchId){return Result.success(matchOverviewService.overview(matchId));}

    @org.springframework.web.bind.annotation.PostMapping("/{matchId}/players/{playerId}/ratings")
    public Result<MatchDataVO.RatingResult> rate(@PathVariable Long matchId,@PathVariable Long playerId,@Valid @org.springframework.web.bind.annotation.RequestBody PlayerRatingRequest request){return Result.success(matchDataService.submitRating(matchId,playerId,request.rating()));}

    @org.springframework.web.bind.annotation.DeleteMapping("/{matchId}/players/{playerId}/ratings")
    public Result<MatchDataVO.RatingResult> cancelRating(@PathVariable Long matchId,@PathVariable Long playerId){return Result.success(matchDataService.cancelRating(matchId,playerId));}
}
