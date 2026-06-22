package com.ccsurvey.common.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

/**
 * Redis工具类
 */
@Slf4j
@Component
public class RedisUtils {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    // ==================== 基础操作 ====================

    /**
     * 设置值
     */
    public void set(String key, String value) {
        stringRedisTemplate.opsForValue().set(key, value);
    }

    /**
     * 设置值并设置过期时间
     */
    public void set(String key, String value, long timeout, TimeUnit unit) {
        stringRedisTemplate.opsForValue().set(key, value, timeout, unit);
    }

    /**
     * 获取值
     */
    public String get(String key) {
        return stringRedisTemplate.opsForValue().get(key);
    }

    /**
     * 删除键
     */
    public Boolean delete(String key) {
        return stringRedisTemplate.delete(key);
    }

    /**
     * 检查键是否存在
     */
    public Boolean hasKey(String key) {
        return stringRedisTemplate.hasKey(key);
    }

    /**
     * 设置过期时间
     */
    public Boolean expire(String key, long timeout, TimeUnit unit) {
        return stringRedisTemplate.expire(key, timeout, unit);
    }

    // ==================== 计数器 ====================

    /**
     * 递增
     */
    public Long increment(String key) {
        return stringRedisTemplate.opsForValue().increment(key);
    }

    /**
     * 递增指定值
     */
    public Long increment(String key, long delta) {
        return stringRedisTemplate.opsForValue().increment(key, delta);
    }

    /**
     * 递减
     */
    public Long decrement(String key) {
        return stringRedisTemplate.opsForValue().decrement(key);
    }

    // ==================== 限流 ====================

    /**
     * 检查是否超过限流阈值
     *
     * @param key          限流键
     * @param maxRequests  最大请求数
     * @param windowSeconds 时间窗口(秒)
     * @return true-允许请求, false-被限流
     */
    public boolean checkRateLimit(String key, int maxRequests, int windowSeconds) {
        String rateKey = "rate_limit:" + key;
        Long count = stringRedisTemplate.opsForValue().increment(rateKey);

        if (count != null && count == 1) {
            // 第一次请求，设置过期时间
            stringRedisTemplate.expire(rateKey, windowSeconds, TimeUnit.SECONDS);
        }

        return count != null && count <= maxRequests;
    }

    /**
     * 获取当前计数
     */
    public Long getRateLimitCount(String key) {
        String rateKey = "rate_limit:" + key;
        String value = stringRedisTemplate.opsForValue().get(rateKey);
        return value != null ? Long.parseLong(value) : 0L;
    }

    /**
     * 获取限流剩余时间(秒)
     */
    public Long getRateLimitTTL(String key) {
        String rateKey = "rate_limit:" + key;
        return stringRedisTemplate.getExpire(rateKey, TimeUnit.SECONDS);
    }

    // ==================== 分布式锁 ====================

    /**
     * 尝试获取分布式锁
     *
     * @param lockKey 锁键
     * @param value   锁值(用于释放锁时验证)
     * @param expireSeconds 锁过期时间(秒)
     * @return true-获取成功, false-获取失败
     */
    public boolean tryLock(String lockKey, String value, int expireSeconds) {
        String key = "lock:" + lockKey;
        Boolean result = stringRedisTemplate.opsForValue()
                .setIfAbsent(key, value, expireSeconds, TimeUnit.SECONDS);
        return Boolean.TRUE.equals(result);
    }

    /**
     * 释放分布式锁 (Lua脚本保证原子性)
     */
    public boolean releaseLock(String lockKey, String value) {
        String key = "lock:" + lockKey;
        String script = "if redis.call('get', KEYS[1]) == ARGV[1] then " +
                "return redis.call('del', KEYS[1]) " +
                "else return 0 end";
        DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>(script, Long.class);
        Long result = stringRedisTemplate.execute(redisScript, Collections.singletonList(key), value);
        return Long.valueOf(1).equals(result);
    }

    /**
     * 检查锁是否存在
     */
    public boolean isLocked(String lockKey) {
        String key = "lock:" + lockKey;
        return Boolean.TRUE.equals(stringRedisTemplate.hasKey(key));
    }

    // ==================== 缓存操作 ====================

    /**
     * 缓存键前缀
     */
    private static final String CACHE_PREFIX = "cache:";

    /**
     * 设置缓存
     */
    public void setCache(String cacheKey, String value, long timeoutMinutes) {
        String key = CACHE_PREFIX + cacheKey;
        stringRedisTemplate.opsForValue().set(key, value, timeoutMinutes, TimeUnit.MINUTES);
    }

    /**
     * 获取缓存
     */
    public String getCache(String cacheKey) {
        String key = CACHE_PREFIX + cacheKey;
        return stringRedisTemplate.opsForValue().get(key);
    }

    /**
     * 删除缓存
     */
    public void deleteCache(String cacheKey) {
        String key = CACHE_PREFIX + cacheKey;
        stringRedisTemplate.delete(key);
    }

    // ==================== 会话管理 ====================

    private static final String SESSION_PREFIX = "session:";

    /**
     * 存储用户会话
     */
    public void storeUserSession(String userId, String token, long expirationMs) {
        String key = SESSION_PREFIX + userId;
        stringRedisTemplate.opsForSet().add(key, token);
        stringRedisTemplate.expire(key, expirationMs, TimeUnit.MILLISECONDS);
    }

    /**
     * 检查Token是否有效
     */
    public boolean isTokenInSession(String userId, String token) {
        String key = SESSION_PREFIX + userId;
        return Boolean.TRUE.equals(stringRedisTemplate.opsForSet().isMember(key, token));
    }

    /**
     * 使会话失效 (登出)
     */
    public void invalidateSession(String userId) {
        String key = SESSION_PREFIX + userId;
        stringRedisTemplate.delete(key);
    }

    /**
     * 将Token加入黑名单
     */
    public void addToBlacklist(String token, long expirationMs) {
        String key = "blacklist:" + token;
        stringRedisTemplate.opsForValue().set(key, "1", expirationMs, TimeUnit.MILLISECONDS);
    }

    /**
     * 检查Token是否在黑名单中
     */
    public boolean isBlacklisted(String token) {
        String key = "blacklist:" + token;
        return Boolean.TRUE.equals(stringRedisTemplate.hasKey(key));
    }

    // ==================== 提交计数器 ====================

    private static final String SUBMISSION_COUNT_PREFIX = "submission_count:";
    private static final String SUBMISSION_LOCK_PREFIX = "submission_lock:";

    /**
     * 原子递增提交计数器并检查是否超限
     * 使用 Lua 脚本保证原子性
     *
     * @param templateUuid   问卷UUID
     * @param maxSubmissions 最大提交数
     * @param expireSeconds  计数器过期时间(秒)
     * @return true-递增成功且未超限, false-已达上限
     */
    public boolean incrementSubmissionCount(String templateUuid, int maxSubmissions, int expireSeconds) {
        String key = SUBMISSION_COUNT_PREFIX + templateUuid;
        String script = """
                local current = redis.call('GET', KEYS[1])
                if current == false then
                    current = 0
                else
                    current = tonumber(current)
                end
                if current >= tonumber(ARGV[1]) then
                    return -1
                end
                redis.call('INCR', KEYS[1])
                redis.call('EXPIRE', KEYS[1], tonumber(ARGV[2]))
                return 1
                """;
        DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>(script, Long.class);
        Long result = stringRedisTemplate.execute(redisScript,
                Collections.singletonList(key),
                String.valueOf(maxSubmissions),
                String.valueOf(expireSeconds));
        return Long.valueOf(1L).equals(result);
    }

    /**
     * 递减提交计数器（用于回滚）
     *
     * @param templateUuid 问卷UUID
     */
    public void decrementSubmissionCount(String templateUuid) {
        String key = SUBMISSION_COUNT_PREFIX + templateUuid;
        String script = """
                local current = redis.call('GET', KEYS[1])
                if current ~= false and tonumber(current) > 0 then
                    redis.call('DECR', KEYS[1])
                end
                return 1
                """;
        DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>(script, Long.class);
        stringRedisTemplate.execute(redisScript, Collections.singletonList(key));
    }

    /**
     * 获取当前提交计数
     *
     * @param templateUuid 问卷UUID
     * @return 当前提交数，如果计数器不存在返回null
     */
    public Long getSubmissionCount(String templateUuid) {
        String key = SUBMISSION_COUNT_PREFIX + templateUuid;
        String value = stringRedisTemplate.opsForValue().get(key);
        return value != null ? Long.parseLong(value) : null;
    }

    /**
     * 初始化提交计数器
     *
     * @param templateUuid  问卷UUID
     * @param count         初始计数
     * @param expireSeconds 过期时间(秒)
     */
    public void initSubmissionCount(String templateUuid, long count, int expireSeconds) {
        String key = SUBMISSION_COUNT_PREFIX + templateUuid;
        stringRedisTemplate.opsForValue().set(key, String.valueOf(count), expireSeconds, TimeUnit.SECONDS);
        log.debug("Initialized submission count for {}: {}", templateUuid, count);
    }

    /**
     * 删除提交计数器
     *
     * @param templateUuid 问卷UUID
     */
    public void deleteSubmissionCount(String templateUuid) {
        String key = SUBMISSION_COUNT_PREFIX + templateUuid;
        stringRedisTemplate.delete(key);
    }

    /**
     * 尝试获取问卷提交锁
     *
     * @param templateUuid  问卷UUID
     * @param lockValue     锁值(用于释放锁时验证)
     * @param expireSeconds 锁过期时间(秒)
     * @return true-获取成功, false-获取失败
     */
    public boolean trySubmissionLock(String templateUuid, String lockValue, int expireSeconds) {
        String key = SUBMISSION_LOCK_PREFIX + templateUuid;
        Boolean result = stringRedisTemplate.opsForValue()
                .setIfAbsent(key, lockValue, expireSeconds, TimeUnit.SECONDS);
        return Boolean.TRUE.equals(result);
    }

    /**
     * 释放问卷提交锁
     *
     * @param templateUuid 问卷UUID
     * @param lockValue    锁值
     * @return true-释放成功, false-释放失败(锁不存在或值不匹配)
     */
    public boolean releaseSubmissionLock(String templateUuid, String lockValue) {
        String key = SUBMISSION_LOCK_PREFIX + templateUuid;
        String script = "if redis.call('get', KEYS[1]) == ARGV[1] then " +
                "return redis.call('del', KEYS[1]) " +
                "else return 0 end";
        DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>(script, Long.class);
        Long result = stringRedisTemplate.execute(redisScript, Collections.singletonList(key), lockValue);
        return Long.valueOf(1L).equals(result);
    }
}