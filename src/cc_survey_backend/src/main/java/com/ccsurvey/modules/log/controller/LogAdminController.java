package com.ccsurvey.modules.log.controller;

import com.ccsurvey.common.response.ApiResponse;
import com.ccsurvey.common.response.PageResponse;
import com.ccsurvey.modules.log.entity.OperationLog;
import com.ccsurvey.modules.log.service.LogService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

/**
 * 日志管理控制器 (管理员)
 */
@RestController
@RequestMapping("/api/admin/logs")
@RequiredArgsConstructor
public class LogAdminController {

    private final LogService logService;

    /**
     * 获取日志列表
     */
    @GetMapping
    public ApiResponse<PageResponse<OperationLog>> getLogList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String businessType,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime) {

        PageResponse<OperationLog> result = logService.getLogList(page, size, title, businessType, status, startTime, endTime);
        return ApiResponse.success(result);
    }

    /**
     * 获取日志详情
     */
    @GetMapping("/{id}")
    public ApiResponse<OperationLog> getLog(@PathVariable Long id) {
        OperationLog log = logService.getLogById(id);
        return ApiResponse.success(log);
    }

    /**
     * 删除日志
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteLog(@PathVariable Long id) {
        logService.deleteLog(id);
        return ApiResponse.success("日志删除成功");
    }

    /**
     * 清空历史日志
     */
    @DeleteMapping("/clear")
    public ApiResponse<Void> clearLogs(@RequestParam(defaultValue = "30") int retainDays) {
        logService.clearLogs(retainDays);
        return ApiResponse.success("历史日志已清空");
    }
}