package com.southstand.interaction.controller;

import com.southstand.common.result.Result;
import com.southstand.interaction.dto.ToggleFavoriteRequest;
import com.southstand.interaction.service.InteractionService;
import com.southstand.interaction.vo.ToggleFavoriteResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/app/favorites")
public class FavoriteController {

    private final InteractionService interactionService;

    public FavoriteController(InteractionService interactionService) {
        this.interactionService = interactionService;
    }

    @PostMapping("/toggle")
    public Result<ToggleFavoriteResponse> toggle(@Valid @RequestBody ToggleFavoriteRequest request) {
        return Result.success(interactionService.toggleFavorite(request));
    }
}
