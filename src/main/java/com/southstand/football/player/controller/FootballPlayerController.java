package com.southstand.football.player.controller;

import com.southstand.common.result.Result;
import com.southstand.football.player.vo.PlayerDetailVO;
import com.southstand.football.schedule.service.FootballQueryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/app/football/players")
public class FootballPlayerController {

    private final FootballQueryService footballQueryService;

    public FootballPlayerController(FootballQueryService footballQueryService) {
        this.footballQueryService = footballQueryService;
    }

    @GetMapping("/{playerId}")
    public Result<PlayerDetailVO> playerDetail(@PathVariable Long playerId) {
        return Result.success(footballQueryService.playerDetail(playerId));
    }
}
