package com.southstand.content.controller;

import com.southstand.common.result.PageResult;
import com.southstand.common.result.Result;
import com.southstand.content.service.PublishSubjectService;
import com.southstand.content.vo.PublishSubjectVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/app/publish/subjects")
public class PublishSubjectController {

    private final PublishSubjectService service;

    public PublishSubjectController(PublishSubjectService service) {
        this.service = service;
    }

    @GetMapping
    public Result<PageResult<PublishSubjectVO>> list(
            @RequestParam String type,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false, defaultValue = "1") Long pageNum,
            @RequestParam(required = false, defaultValue = "20") Long pageSize) {
        return Result.success(service.list(type, keyword, pageNum, pageSize));
    }
}
