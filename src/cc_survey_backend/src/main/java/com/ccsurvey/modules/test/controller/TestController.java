package com.ccsurvey.modules.test.controller;

import com.ccsurvey.common.response.ApiResponse;
import com.ccsurvey.common.util.RedisUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 测试接口控制器
 * 仅在test profile下启用
 */
@Slf4j
@RestController
@RequestMapping("/test")
@RequiredArgsConstructor
@Profile("test")
public class TestController {

    private final RedisUtils redisUtils;

    // ==================== Redis测试接口 ====================

    /**
     * Redis写入测试
     */
    @GetMapping("/redis/set")
    public ApiResponse<Map<String, Object>> testRedisSet(
            @RequestParam String key,
            @RequestParam String value) {

        String fullKey = "test:" + key;
        redisUtils.set(fullKey, value);

        Map<String, Object> result = new HashMap<>();
        result.put("key", fullKey);
        result.put("value", value);
        result.put("success", true);

        return ApiResponse.success(result);
    }

    /**
     * Redis读取测试
     */
    @GetMapping("/redis/get")
    public ApiResponse<Map<String, Object>> testRedisGet(@RequestParam String key) {
        String fullKey = "test:" + key;
        String value = redisUtils.get(fullKey);

        Map<String, Object> result = new HashMap<>();
        result.put("key", fullKey);
        result.put("value", value);
        result.put("exists", value != null);

        return ApiResponse.success(result);
    }

    /**
     * Redis删除测试
     */
    @DeleteMapping("/redis/delete")
    public ApiResponse<Map<String, Object>> testRedisDelete(@RequestParam String key) {
        String fullKey = "test:" + key;
        Boolean deleted = redisUtils.delete(fullKey);

        Map<String, Object> result = new HashMap<>();
        result.put("key", fullKey);
        result.put("deleted", deleted);

        return ApiResponse.success(result);
    }

    /**
     * Redis计数器测试
     */
    @GetMapping("/redis/incr")
    public ApiResponse<Map<String, Object>> testRedisIncr(@RequestParam String key) {
        String fullKey = "test:counter:" + key;
        Long value = redisUtils.increment(fullKey);

        Map<String, Object> result = new HashMap<>();
        result.put("key", fullKey);
        result.put("value", value);

        return ApiResponse.success(result);
    }

    // ==================== 限流测试接口 ====================

    /**
     * 限流测试
     */
    @GetMapping("/rate-limit")
    public ApiResponse<Map<String, Object>> testRateLimit(
            @RequestParam(defaultValue = "10") int maxRequests,
            @RequestParam(defaultValue = "60") int windowSeconds,
            jakarta.servlet.http.HttpServletRequest request) {

        String clientIp = com.ccsurvey.common.util.IpUtils.getClientIp(request);
        String key = "test:" + clientIp;

        boolean allowed = redisUtils.checkRateLimit(key, maxRequests, windowSeconds);
        Long currentCount = redisUtils.getRateLimitCount(key);
        Long ttl = redisUtils.getRateLimitTTL(key);

        Map<String, Object> result = new HashMap<>();
        result.put("clientIp", clientIp);
        result.put("allowed", allowed);
        result.put("currentCount", currentCount);
        result.put("maxRequests", maxRequests);
        result.put("resetInSeconds", ttl);

        return ApiResponse.success(result);
    }

    // ==================== 分布式锁测试接口 ====================

    /**
     * 分布式锁测试
     */
    @GetMapping("/lock/acquire")
    public ApiResponse<Map<String, Object>> testLockAcquire(@RequestParam String resourceId) {
        String lockKey = "test:" + resourceId;
        String lockValue = UUID.randomUUID().toString();

        boolean acquired = redisUtils.tryLock(lockKey, lockValue, 30);

        Map<String, Object> result = new HashMap<>();
        result.put("resourceId", resourceId);
        result.put("acquired", acquired);
        result.put("lockValue", lockValue);

        return ApiResponse.success(result);
    }

    /**
     * 分布式锁释放测试
     */
    @DeleteMapping("/lock/release")
    public ApiResponse<Map<String, Object>> testLockRelease(
            @RequestParam String resourceId,
            @RequestParam String lockValue) {

        String lockKey = "test:" + resourceId;
        boolean released = redisUtils.releaseLock(lockKey, lockValue);

        Map<String, Object> result = new HashMap<>();
        result.put("resourceId", resourceId);
        result.put("released", released);

        return ApiResponse.success(result);
    }

    // ==================== 并发测试接口 ====================

    /**
     * 并发提交测试
     */
    @PostMapping("/concurrent/submit")
    public ApiResponse<Map<String, Object>> testConcurrentSubmit(
            @RequestParam String templateUuid,
            @RequestParam(defaultValue = "1") int count) {

        // 模拟并发提交
        Map<String, Object> result = new HashMap<>();
        result.put("templateUuid", templateUuid);
        result.put("requestedCount", count);
        result.put("message", "Use this endpoint with JMeter or other load testing tools");

        return ApiResponse.success(result);
    }

    // ==================== 健康检查接口 ====================

    /**
     * 测试接口健康检查
     */
    @GetMapping("/health")
    public ApiResponse<Map<String, Object>> healthCheck() {
        Map<String, Object> result = new HashMap<>();
        result.put("status", "UP");
        result.put("timestamp", System.currentTimeMillis());
        result.put("profile", "test");

        return ApiResponse.success(result);
    }
}