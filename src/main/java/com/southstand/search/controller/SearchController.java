package com.southstand.search.controller;

import com.southstand.common.result.PageResult;
import com.southstand.common.result.Result;
import com.southstand.search.service.SearchService;
import com.southstand.search.vo.SearchEntityVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/app/search")
public class SearchController {

    private final SearchService searchService;

    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    @GetMapping("/entities")
    public Result<PageResult<SearchEntityVO>> entities(
            @RequestParam String keyword,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false, defaultValue = "1") Long pageNum,
            @RequestParam(required = false, defaultValue = "20") Long pageSize) {
        return Result.success(searchService.entities(keyword, entityType, pageNum, pageSize));
    }
}
