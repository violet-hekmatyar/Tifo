package com.southstand.admin.controller;

import com.southstand.admin.dto.AdminContentStatusRequest;
import com.southstand.admin.service.AdminService;
import com.southstand.admin.vo.AdminContentVO;
import com.southstand.admin.vo.AdminStatusUpdateVO;
import com.southstand.common.result.PageResult;
import com.southstand.common.result.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/contents")
public class AdminContentController {

    private final AdminService adminService;

    public AdminContentController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping
    public Result<PageResult<AdminContentVO>> list(
            @RequestParam(required = false, defaultValue = "1") Long pageNum,
            @RequestParam(required = false, defaultValue = "10") Long pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String contentType,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long authorId) {
        return Result.success(adminService.contents(pageNum, pageSize, keyword, contentType, status, authorId));
    }

    @PutMapping("/{contentId}/status")
    public Result<AdminStatusUpdateVO> updateStatus(@PathVariable Long contentId,
            @RequestBody AdminContentStatusRequest request) {
        return Result.success(adminService.updateContentStatus(contentId, request));
    }
}
