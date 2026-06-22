package com.ccsurvey.modules.log.service;

import com.ccsurvey.modules.log.entity.OperationLog;
import com.ccsurvey.modules.log.repository.OperationLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * 异步日志服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AsyncLogService {

    private final OperationLogRepository logRepository;

    /**
     * 异步保存日志
     */
    @Async("logExecutor")
    public void saveLog(OperationLog operationLog) {
        try {
            logRepository.insert(operationLog);
        } catch (Exception e) {
            log.error("保存操作日志失败", e);
        }
    }
}