package com.ccsurvey.modules.auth.security;

import com.ccsurvey.common.response.ErrorCode;
import com.ccsurvey.common.util.JwtUtils;
import com.ccsurvey.common.util.RedisUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT认证过滤器
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtils jwtUtils;
    private final RedisUtils redisUtils;
    private final ObjectMapper objectMapper;

    private static final String COOKIE_NAME = "cc_survey_token";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // 放行OPTIONS预检请求
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        // 设置请求开始时间 (用于日志记录耗时)
        request.setAttribute("startTime", System.currentTimeMillis());

        String path = request.getRequestURI();

        // 从Cookie获取Token
        String token = getTokenFromCookie(request);

        if (token == null) {
            // 尝试从Header获取 (兼容API调用)
            String authHeader = request.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                token = authHeader.substring(7);
            }
        }

        // 如果有token，验证并提取用户信息
        if (token != null && jwtUtils.validateToken(token) && !redisUtils.isBlacklisted(token)) {
            // 从Token中提取用户信息
            String userId = jwtUtils.getUserId(token);
            String username = jwtUtils.getUsername(token);
            String name = jwtUtils.getName(token);
            String role = jwtUtils.getRole(token);

            // 将用户信息存入请求属性
            request.setAttribute("userId", userId);
            request.setAttribute("username", username);
            request.setAttribute("name", name);
            request.setAttribute("role", role);
        }

        // 放行公开接口（不需要强制登录）
        if (isPublicPath(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        // 非公开接口需要验证登录
        if (token == null) {
            sendUnauthorizedResponse(response, ErrorCode.TOKEN_MISSING);
            return;
        }

        if (!jwtUtils.validateToken(token)) {
            sendUnauthorizedResponse(response, ErrorCode.TOKEN_INVALID);
            return;
        }

        if (redisUtils.isBlacklisted(token)) {
            sendUnauthorizedResponse(response, ErrorCode.TOKEN_INVALID);
            return;
        }

        filterChain.doFilter(request, response);
    }

    /**
     * 判断是否为公开路径
     */
    private boolean isPublicPath(String path) {
        return path.startsWith("/api/auth/login") ||
                path.startsWith("/api/auth/register") ||
                path.startsWith("/api/auth/verify-email") ||
                path.startsWith("/api/auth/password/reset") ||
                path.startsWith("/api/auth/password/reset-request") ||
                path.startsWith("/api/auth/refresh") ||
                path.startsWith("/api/public/") ||
                path.startsWith("/api/test/") ||
                path.startsWith("/api/surveys/") ||
                path.equals("/api/submit") ||  // 提交接口支持匿名，但需要记录日志
                path.startsWith("/files/") ||
                path.startsWith("/actuator/health") ||
                path.startsWith("/error");
    }

    /**
     * 从Cookie获取Token
     */
    private String getTokenFromCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }

        for (Cookie cookie : cookies) {
            if (COOKIE_NAME.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }

        return null;
    }

    /**
     * 发送未授权响应
     */
    private void sendUnauthorizedResponse(HttpServletResponse response, ErrorCode errorCode) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");

        Map<String, Object> result = new HashMap<>();
        result.put("code", errorCode.getCode());
        result.put("message", errorCode.getMessage());
        result.put("data", null);
        result.put("timestamp", System.currentTimeMillis());

        response.getWriter().write(objectMapper.writeValueAsString(result));
    }
}