package com.southstand.content.controller;

import com.southstand.common.result.Result;
import com.southstand.content.dto.CreatePostRequest;
import com.southstand.content.service.ContentService;
import com.southstand.content.vo.ContentDetailVO;
import com.southstand.content.vo.CreatePostResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/app/contents")
public class ContentController {

    private final ContentService contentService;

    public ContentController(ContentService contentService) {
        this.contentService = contentService;
    }

    @GetMapping("/{contentId}")
    public Result<ContentDetailVO> detail(@PathVariable Long contentId) {
        return Result.success(contentService.detail(contentId));
    }

    @PostMapping("/posts")
    public Result<CreatePostResponse> createPost(@Valid @RequestBody CreatePostRequest request) {
        return Result.success(contentService.createPost(request));
    }
}
