package com.southstand.user.controller;

import com.southstand.common.result.PageResult;
import com.southstand.common.result.Result;
import com.southstand.user.service.UserSocialService;
import com.southstand.user.vo.MyCommentVO;
import com.southstand.user.vo.MyContentVO;
import com.southstand.user.vo.MyFavoriteVO;
import com.southstand.user.vo.UserFollowActionVO;
import com.southstand.user.vo.UserFollowItemVO;
import com.southstand.user.vo.UserPublicProfileVO;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/app/users")
public class UserSocialController {

    private final UserSocialService userSocialService;

    public UserSocialController(UserSocialService userSocialService) {
        this.userSocialService = userSocialService;
    }

    @GetMapping("/{userId}/profile")
    public Result<UserPublicProfileVO> profile(@PathVariable Long userId) {
        return Result.success(userSocialService.publicProfile(userId));
    }

    @PostMapping("/{userId}/follow")
    public Result<UserFollowActionVO> follow(@PathVariable Long userId) {
        return Result.success(userSocialService.follow(userId));
    }

    @DeleteMapping("/{userId}/follow")
    public Result<UserFollowActionVO> unfollow(@PathVariable Long userId) {
        return Result.success(userSocialService.unfollow(userId));
    }

    @GetMapping("/{userId}/followings")
    public Result<PageResult<UserFollowItemVO>> followings(
            @PathVariable Long userId,
            @RequestParam(required = false, defaultValue = "1") Long pageNum,
            @RequestParam(required = false, defaultValue = "10") Long pageSize) {
        return Result.success(userSocialService.followings(userId, pageNum, pageSize));
    }

    @GetMapping("/{userId}/followers")
    public Result<PageResult<UserFollowItemVO>> followers(
            @PathVariable Long userId,
            @RequestParam(required = false, defaultValue = "1") Long pageNum,
            @RequestParam(required = false, defaultValue = "10") Long pageSize) {
        return Result.success(userSocialService.followers(userId, pageNum, pageSize));
    }

    @GetMapping("/{userId}/contents")
    public Result<PageResult<MyContentVO>> contents(
            @PathVariable Long userId,
            @RequestParam(required = false, defaultValue = "1") Long pageNum,
            @RequestParam(required = false, defaultValue = "10") Long pageSize) {
        return Result.success(userSocialService.contents(userId, pageNum, pageSize));
    }

    @GetMapping("/{userId}/favorites")
    public Result<PageResult<MyFavoriteVO>> favorites(
            @PathVariable Long userId,
            @RequestParam(required = false, defaultValue = "1") Long pageNum,
            @RequestParam(required = false, defaultValue = "10") Long pageSize,
            @RequestParam(required = false, defaultValue = "CONTENT") String targetType) {
        return Result.success(userSocialService.favorites(userId, pageNum, pageSize, targetType));
    }

    @GetMapping("/{userId}/comments")
    public Result<PageResult<MyCommentVO>> comments(
            @PathVariable Long userId,
            @RequestParam(required = false, defaultValue = "1") Long pageNum,
            @RequestParam(required = false, defaultValue = "10") Long pageSize,
            @RequestParam(required = false, defaultValue = "CONTENT") String targetType) {
        return Result.success(userSocialService.comments(userId, pageNum, pageSize, targetType));
    }
}
