package com.southstand.onboarding.controller;

import com.southstand.common.result.Result;
import com.southstand.onboarding.dto.SavePreferencesRequest;
import com.southstand.onboarding.service.OnboardingService;
import com.southstand.onboarding.vo.OnboardingOptionsVO;
import com.southstand.onboarding.vo.SavePreferencesResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/app/onboarding")
public class OnboardingController {

    private final OnboardingService onboardingService;

    public OnboardingController(OnboardingService onboardingService) {
        this.onboardingService = onboardingService;
    }

    @GetMapping("/options")
    public Result<OnboardingOptionsVO> options() {
        return Result.success(onboardingService.options());
    }

    @PostMapping("/preferences")
    public Result<SavePreferencesResponse> savePreferences(@Valid @RequestBody SavePreferencesRequest request) {
        return Result.success(onboardingService.savePreferences(request));
    }
}
