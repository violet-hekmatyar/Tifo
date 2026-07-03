package com.southstand.interaction.controller;

import com.southstand.common.result.Result;
import com.southstand.interaction.dto.ToggleLikeRequest;
import com.southstand.interaction.service.InteractionService;
import com.southstand.interaction.vo.ToggleLikeResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/app/likes")
public class LikeController {

    private final InteractionService interactionService;

    public LikeController(InteractionService interactionService) {
        this.interactionService = interactionService;
    }

    @PostMapping("/toggle")
    public Result<ToggleLikeResponse> toggle(@Valid @RequestBody ToggleLikeRequest request) {
        return Result.success(interactionService.toggleLike(request));
    }
}
