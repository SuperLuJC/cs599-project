package com.ccsurvey.modules.log.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ccsurvey.common.response.PageResponse;
import com.ccsurvey.modules.log.entity.OperationLog;
import com.ccsurvey.modules.log.repository.OperationLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 日志服务
 */
@Service
@RequiredArgsConstructor
public class LogService {

    private final OperationLogRepository logRepository;

    /**
     * 获取日志列表 (分页)
     */
    public PageResponse<OperationLog> getLogList(int page, int size, String title,
                                                   String businessType, Integer status,
                                                   LocalDateTime startTime, LocalDateTime endTime) {
        LambdaQueryWrapper<OperationLog> wrapper = new LambdaQueryWrapper<>();

        if (title != null && !title.isEmpty()) {
            wrapper.like(OperationLog::getTitle, title);
        }

        if (businessType != null && !businessType.isEmpty()) {
            wrapper.eq(OperationLog::getBusinessType, businessType);
        }

        if (status != null) {
            wrapper.eq(OperationLog::getStatus, status);
        }

        if (startTime != null) {
            wrapper.ge(OperationLog::getOperTime, startTime);
        }

        if (endTime != null) {
            wrapper.le(OperationLog::getOperTime, endTime);
        }

        wrapper.orderByDesc(OperationLog::getOperTime);

        Page<OperationLog> pageResult = logRepository.selectPage(new Page<>(page, size), wrapper);

        return PageResponse.of(pageResult.getRecords(), pageResult.getTotal(), page, size);
    }

    /**
     * 获取日志详情
     */
    public OperationLog getLogById(Long id) {
        return logRepository.selectById(id);
    }

    /**
     * 删除日志
     */
    public void deleteLog(Long id) {
        logRepository.deleteById(id);
    }

    /**
     * 清空日志 (保留最近N天)
     */
    public void clearLogs(int retainDays) {
        LocalDateTime cutoffTime = LocalDateTime.now().minusDays(retainDays);

        LambdaQueryWrapper<OperationLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.lt(OperationLog::getOperTime, cutoffTime);

        logRepository.delete(wrapper);
    }
}