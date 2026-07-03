package com.southstand.card.controller;

import com.southstand.card.service.FeedService;
import com.southstand.card.vo.FeedPageResult;
import com.southstand.card.vo.HotLeagueVO;
import com.southstand.common.result.Result;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/app/feed")
public class FeedController {

    private final FeedService feedService;

    public FeedController(FeedService feedService) {
        this.feedService = feedService;
    }

    @GetMapping
    public Result<FeedPageResult> feed(
            @RequestParam(required = false, defaultValue = "recommend") String tab,
            @RequestParam(required = false) Long leagueId,
            @RequestParam(required = false) Long teamId,
            @RequestParam(required = false, defaultValue = "1") long pageNum,
            @RequestParam(required = false, defaultValue = "10") long pageSize,
            @RequestParam(required = false) String cursor
    ) {
        return Result.success(feedService.feed(tab, leagueId, teamId, pageNum, pageSize, cursor));
    }

    @GetMapping("/hot-leagues")
    public Result<List<HotLeagueVO>> hotLeagues(@RequestParam(required = false, defaultValue = "10") int limit) {
        return Result.success(feedService.hotLeagues(limit));
    }
}
