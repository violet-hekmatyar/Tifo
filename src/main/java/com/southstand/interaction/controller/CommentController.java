package com.southstand.interaction.controller;

import com.southstand.common.result.PageResult;
import com.southstand.common.result.Result;
import com.southstand.interaction.dto.CreateCommentRequest;
import com.southstand.interaction.service.CommentService;
import com.southstand.interaction.vo.CommentLikeToggleVO;
import com.southstand.interaction.vo.CommentVO;
import com.southstand.interaction.vo.CreateCommentResponse;
import com.southstand.interaction.vo.HotCommentVO;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/app/comments")
public class CommentController {

    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    @GetMapping
    public Result<PageResult<CommentVO>> list(@RequestParam(required = false) String targetType,
            @RequestParam(required = false) Long targetId,
            @RequestParam(required = false) Long contentId,
            @RequestParam(required = false, defaultValue = "hot") String sort,
            @RequestParam(required = false) Long parentId,
            @RequestParam(required = false, defaultValue = "1") Long pageNum,
            @RequestParam(required = false, defaultValue = "10") Long pageSize) {
        Long resolvedTargetId = targetId == null ? contentId : targetId;
        String resolvedTargetType = targetType == null || targetType.isBlank() ? CommentService.TARGET_CONTENT : targetType;
        return Result.success(commentService.list(resolvedTargetType, resolvedTargetId, sort, parentId, pageNum, pageSize));
    }

    @GetMapping("/{commentId}/replies")
    public Result<PageResult<CommentVO>> replies(
            @PathVariable Long commentId,
            @RequestParam(required = false, defaultValue = "latest") String sort,
            @RequestParam(required = false, defaultValue = "1") Long pageNum,
            @RequestParam(required = false, defaultValue = "10") Long pageSize) {
        return Result.success(commentService.replies(commentId, sort, pageNum, pageSize));
    }

    @GetMapping("/hot")
    public Result<List<HotCommentVO>> hot(
            @RequestParam Long contentId,
            @RequestParam(required = false, defaultValue = "3") Integer limit) {
        return Result.success(commentService.hotComments(contentId, limit == null ? 3 : limit));
    }

    @PostMapping
    public Result<CreateCommentResponse> create(@Valid @RequestBody CreateCommentRequest request) {
        return Result.success(commentService.create(request));
    }

    @PostMapping("/{commentId}/likes/toggle")
    public Result<CommentLikeToggleVO> toggleLike(@PathVariable Long commentId) {
        return Result.success(commentService.toggleLike(commentId));
    }

    @DeleteMapping("/{commentId}")
    public Result<Boolean> delete(@PathVariable Long commentId) {
        return Result.success(commentService.delete(commentId));
    }
}
