package com.southstand.admin.controller;

import com.southstand.admin.dto.AdminUserStatusRequest;
import com.southstand.admin.service.AdminService;
import com.southstand.admin.vo.AdminStatusUpdateVO;
import com.southstand.admin.vo.AdminUserVO;
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
@RequestMapping("/api/admin/users")
public class AdminUserController {

    private final AdminService adminService;

    public AdminUserController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping
    public Result<PageResult<AdminUserVO>> list(
            @RequestParam(required = false, defaultValue = "1") Long pageNum,
            @RequestParam(required = false, defaultValue = "10") Long pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String roleType) {
        return Result.success(adminService.users(pageNum, pageSize, keyword, status, roleType));
    }

    @PutMapping("/{userId}/status")
    public Result<AdminStatusUpdateVO> updateStatus(@PathVariable Long userId,
            @RequestBody AdminUserStatusRequest request) {
        return Result.success(adminService.updateUserStatus(userId, request));
    }
}
