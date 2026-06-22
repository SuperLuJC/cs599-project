package com.ccsurvey.modules.statistics.controller;

import com.ccsurvey.common.response.ApiResponse;
import com.ccsurvey.modules.statistics.dto.DashboardStatsDTO;
import com.ccsurvey.modules.statistics.service.StatisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 统计控制器 (管理员)
 */
@RestController
@RequestMapping("/api/admin/statistics")
@RequiredArgsConstructor
public class StatisticsController {

    private final StatisticsService statisticsService;

    /**
     * 获取仪表盘数据
     */
    @GetMapping("/dashboard")
    public ApiResponse<DashboardStatsDTO> getDashboardStats() {
        DashboardStatsDTO stats = statisticsService.getDashboardStats();
        return ApiResponse.success(stats);
    }

    /**
     * 获取问卷详细统计
     */
    @GetMapping("/survey/{uuid}")
    public ApiResponse<Map<String, Object>> getSurveyStats(@PathVariable String uuid) {
        Map<String, Object> stats = statisticsService.getSurveyStats(uuid);
        return ApiResponse.success(stats);
    }
}