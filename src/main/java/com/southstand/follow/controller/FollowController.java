package com.southstand.follow.controller;

import com.southstand.common.result.Result;
import com.southstand.follow.dto.FollowToggleRequest;
import com.southstand.follow.service.FollowService;
import com.southstand.follow.vo.FollowToggleResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/app/follows")
public class FollowController {

    private final FollowService followService;

    public FollowController(FollowService followService) {
        this.followService = followService;
    }

    @PostMapping("/toggle")
    public Result<FollowToggleResponse> toggle(@Valid @RequestBody FollowToggleRequest request) {
        return Result.success(followService.toggle(request));
    }
}
