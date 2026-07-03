package com.southstand.admin.controller;

import com.southstand.admin.service.AdminService;
import com.southstand.admin.vo.AdminDashboardSummaryVO;
import com.southstand.common.result.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/dashboard")
public class AdminDashboardController {

    private final AdminService adminService;

    public AdminDashboardController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/summary")
    public Result<AdminDashboardSummaryVO> summary() {
        return Result.success(adminService.dashboardSummary());
    }
}
