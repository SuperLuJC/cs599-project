package com.ccsurvey.modules.auth.security;

import com.ccsurvey.common.response.ErrorCode;
import com.ccsurvey.common.util.IpUtils;
import com.ccsurvey.common.util.RedisUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * 限流过滤器
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {

    private final RedisUtils redisUtils;
    private final ObjectMapper objectMapper;

    @Value("${login.rate-limit.enabled:true}")
    private boolean rateLimitEnabled;

    @Value("${login.rate-limit.requests-per-minute:60}")
    private int requestsPerMinute;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        if (!rateLimitEnabled) {
            filterChain.doFilter(request, response);
            return;
        }

        String path = request.getRequestURI();
        String method = request.getMethod();

        // 只对特定接口进行限流
        if (shouldRateLimit(path, method)) {
            String clientIp = IpUtils.getClientIp(request);
            String rateLimitKey = buildRateLimitKey(path, clientIp);

            // 检查限流
            if (!redisUtils.checkRateLimit(rateLimitKey, requestsPerMinute, 60)) {
                log.warn("请求被限流: ip={}, path={}", clientIp, path);
                sendRateLimitResponse(response);
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    /**
     * 判断是否需要限流
     */
    private boolean shouldRateLimit(String path, String method) {
        // 登录接口需要严格限流
        if (path.startsWith("/api/auth/login") && "POST".equals(method)) {
            return true;
        }

        // 注册接口需要限流
        if (path.startsWith("/api/auth/register") && "POST".equals(method)) {
            return true;
        }

        // 提交接口需要限流
        if (path.startsWith("/api/submit") && "POST".equals(method)) {
            return true;
        }

        // 文件上传需要限流
        if (path.startsWith("/api/upload") && "POST".equals(method)) {
            return true;
        }

        return false;
    }

    /**
     * 构建限流键
     */
    private String buildRateLimitKey(String path, String clientIp) {
        // 对于登录接口，使用更严格的限流
        if (path.startsWith("/api/auth/login")) {
            return "login:" + clientIp;
        }
        return path + ":" + clientIp;
    }

    /**
     * 发送限流响应
     */
    private void sendRateLimitResponse(HttpServletResponse response) throws IOException {
        response.setStatus(429);
        response.setContentType("application/json;charset=UTF-8");

        Map<String, Object> result = new HashMap<>();
        result.put("code", ErrorCode.RATE_LIMIT_EXCEEDED.getCode());
        result.put("message", ErrorCode.RATE_LIMIT_EXCEEDED.getMessage());
        result.put("data", null);
        result.put("timestamp", System.currentTimeMillis());

        response.getWriter().write(objectMapper.writeValueAsString(result));
    }
}