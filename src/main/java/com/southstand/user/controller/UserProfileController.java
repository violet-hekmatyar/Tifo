package com.southstand.user.controller;

import com.southstand.common.result.Result;
import com.southstand.common.result.PageResult;
import com.southstand.user.dto.UpdateMyProfileRequest;
import com.southstand.user.service.UserProfileService;
import com.southstand.user.vo.MyCommentVO;
import com.southstand.user.vo.MyContentVO;
import com.southstand.user.vo.MyFavoriteVO;
import com.southstand.user.vo.MyProfileUpdateVO;
import com.southstand.user.vo.UserProfileVO;
import com.southstand.user.vo.UserSummaryVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/app/users/me")
public class UserProfileController {

    private final UserProfileService userProfileService;

    public UserProfileController(UserProfileService userProfileService) {
        this.userProfileService = userProfileService;
    }

    @GetMapping("/profile")
    public Result<UserProfileVO> profile() {
        return Result.success(userProfileService.me());
    }

    @GetMapping("/summary")
    public Result<UserSummaryVO> summary() {
        return Result.success(userProfileService.summary());
    }

    @PutMapping("/profile")
    public Result<MyProfileUpdateVO> updateProfile(@RequestBody UpdateMyProfileRequest request) {
        return Result.success(userProfileService.updateProfile(request));
    }

    @GetMapping("/contents")
    public Result<PageResult<MyContentVO>> contents(
            @RequestParam(required = false, defaultValue = "1") Long pageNum,
            @RequestParam(required = false, defaultValue = "10") Long pageSize,
            @RequestParam(required = false) String contentType) {
        return Result.success(userProfileService.myContents(pageNum, pageSize, contentType));
    }

    @GetMapping("/favorites")
    public Result<PageResult<MyFavoriteVO>> favorites(
            @RequestParam(required = false, defaultValue = "1") Long pageNum,
            @RequestParam(required = false, defaultValue = "10") Long pageSize,
            @RequestParam(required = false, defaultValue = "CONTENT") String targetType) {
        return Result.success(userProfileService.myFavorites(pageNum, pageSize, targetType));
    }

    @GetMapping("/comments")
    public Result<PageResult<MyCommentVO>> comments(
            @RequestParam(required = false, defaultValue = "1") Long pageNum,
            @RequestParam(required = false, defaultValue = "10") Long pageSize,
            @RequestParam(required = false, defaultValue = "CONTENT") String targetType) {
        return Result.success(userProfileService.myComments(pageNum, pageSize, targetType));
    }
}
