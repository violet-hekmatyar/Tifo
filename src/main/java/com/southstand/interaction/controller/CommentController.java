package com.southstand.interaction.controller;

import com.southstand.common.result.PageResult;
import com.southstand.common.result.Result;
import com.southstand.interaction.dto.CreateCommentRequest;
import com.southstand.interaction.service.CommentService;
import com.southstand.interaction.vo.CommentVO;
import com.southstand.interaction.vo.CreateCommentResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
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
    public Result<PageResult<CommentVO>> list(@RequestParam String targetType,
            @RequestParam Long targetId,
            @RequestParam(required = false, defaultValue = "hot") String sort,
            @RequestParam(required = false) Long parentId,
            @RequestParam(required = false, defaultValue = "1") Long pageNum,
            @RequestParam(required = false, defaultValue = "10") Long pageSize) {
        return Result.success(commentService.list(targetType, targetId, sort, parentId, pageNum, pageSize));
    }

    @PostMapping
    public Result<CreateCommentResponse> create(@Valid @RequestBody CreateCommentRequest request) {
        return Result.success(commentService.create(request));
    }
}
