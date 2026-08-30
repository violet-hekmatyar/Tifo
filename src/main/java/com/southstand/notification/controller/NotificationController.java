package com.southstand.notification.controller;

import com.southstand.common.result.PageResult;
import com.southstand.common.result.Result;
import com.southstand.notification.service.NotificationService;
import com.southstand.notification.vo.NotificationVO;
import com.southstand.notification.vo.ReadAllVO;
import com.southstand.notification.vo.UnreadCountVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/app/notifications")
public class NotificationController {
    private final NotificationService service;
    public NotificationController(NotificationService service){this.service=service;}
    @GetMapping public Result<PageResult<NotificationVO>> list(@RequestParam(defaultValue="1") Long pageNum,@RequestParam(defaultValue="10") Long pageSize,@RequestParam(required=false) String type,@RequestParam(required=false) String readStatus){return Result.success(service.list(pageNum,pageSize,type,readStatus));}
    @GetMapping("/unread-count") public Result<UnreadCountVO> unreadCount(){return Result.success(service.unreadCount());}
    @PostMapping("/{notificationId}/read") public Result<Boolean> read(@PathVariable Long notificationId){return Result.success(service.read(notificationId));}
    @PostMapping("/read-all") public Result<ReadAllVO> readAll(){return Result.success(service.readAll());}
}
